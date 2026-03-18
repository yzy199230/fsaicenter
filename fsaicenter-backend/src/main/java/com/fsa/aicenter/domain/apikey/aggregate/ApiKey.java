package com.fsa.aicenter.domain.apikey.aggregate;

import com.fsa.aicenter.domain.apikey.valueobject.AccessControl;
import com.fsa.aicenter.domain.apikey.valueobject.Quota;
import com.fsa.aicenter.domain.apikey.valueobject.RateLimit;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API密钥聚合根
 */
@Getter
@Setter  // Repository需要能够重建聚合根对象
public class ApiKey {
    private Long id;
    private String keyValue;
    private String keyName;
    private String description;
    private Quota quota;
    private RateLimit rateLimit;
    private AccessControl accessControl;
    private LocalDateTime expireTime;
    private EntityStatus status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // ========== 领域行为 ==========

    /**
     * 是否可访问指定模型类型
     */
    public boolean canAccessModelType(ModelType modelType) {
        if (!isValid()) {
            return false;
        }
        return accessControl != null && accessControl.canAccessModelType(modelType);
    }

    /**
     * 是否允许指定IP访问
     */
    public boolean isIpAllowed(String ip) {
        if (!isValid()) {
            return false;
        }
        return accessControl != null && accessControl.isIpAllowed(ip);
    }

    /**
     * 是否有足够配额
     */
    public boolean hasQuota(long requiredAmount) {
        if (!isValid()) {
            return false;
        }
        return quota != null && quota.hasEnough(requiredAmount);
    }

    /**
     * 消费配额
     */
    public void consumeQuota(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Consume amount must be positive, but was: " + amount);
        }
        if (!isValid()) {
            throw new IllegalStateException("Cannot consume quota: API key is not valid");
        }
        if (quota == null) {
            throw new IllegalStateException("Quota is not configured");
        }
        if (!quota.hasEnough(amount)) {
            throw new IllegalStateException("Insufficient quota: required=" + amount + ", available=" + quota.remaining());
        }
        this.quota = quota.consume(amount);
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 退回配额
     */
    public void refundQuota(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive, but was: " + amount);
        }
        if (quota == null) {
            throw new IllegalStateException("Quota is not configured");
        }
        this.quota = quota.refund(amount);
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 获取限流配置
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /**
     * 是否已过期
     */
    public boolean isExpired() {
        if (expireTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return status != null && status.isEnabled();
    }

    /**
     * 是否有效（启用且未过期）
     */
    public boolean isValid() {
        return isEnabled() && !isExpired();
    }

    /**
     * 启用
     */
    public void enable() {
        this.status = EntityStatus.ENABLED;
    }

    /**
     * 禁用
     */
    public void disable() {
        this.status = EntityStatus.DISABLED;
    }
}
