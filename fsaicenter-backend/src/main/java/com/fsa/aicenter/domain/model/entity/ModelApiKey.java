package com.fsa.aicenter.domain.model.entity;

import com.fsa.aicenter.domain.model.valueobject.HealthStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型API Key实体
 * <p>
 * 每个模型可以配置多个API Key，系统自动负载均衡和故障转移
 * </p>
 *
 * @author FSA AI Center
 */
@Data
public class ModelApiKey {

    private Long id;
    private Long modelId;
    private String keyName;
    private String apiKey; // 加密存储
    private Integer weight; // 权重

    // 使用统计
    private Long totalRequests;
    private Long successRequests;
    private Long failedRequests;
    private LocalDateTime lastUsedTime;
    private LocalDateTime lastSuccessTime;
    private LocalDateTime lastFailTime;

    // 健康状态
    private HealthStatus healthStatus;
    private Integer failCount; // 连续失败次数

    // 限流配置
    private Integer rateLimitPerMinute;
    private Integer rateLimitPerDay;

    // 配额
    private Long quotaTotal;
    private Long quotaUsed;

    // 有效期
    private LocalDateTime expireTime;

    private Integer status;
    private Integer sortOrder;
    private String description;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /**
     * 是否健康
     */
    public boolean isHealthy() {
        return healthStatus == HealthStatus.HEALTHY && status == 1;
    }

    /**
     * 是否过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 是否有可用配额
     */
    public boolean hasQuota() {
        if (quotaTotal == null || quotaTotal == -1) {
            return true; // 无限制
        }
        return quotaUsed < quotaTotal;
    }

    /**
     * 是否可用（综合检查）
     */
    public boolean isAvailable() {
        return isHealthy() && !isExpired() && hasQuota();
    }

    /**
     * 记录成功请求
     */
    public void recordSuccess() {
        this.totalRequests++;
        this.successRequests++;
        this.lastUsedTime = LocalDateTime.now();
        this.lastSuccessTime = LocalDateTime.now();
        this.failCount = 0; // 重置失败计数
        if (this.healthStatus == HealthStatus.UNHEALTHY) {
            this.healthStatus = HealthStatus.HEALTHY; // 恢复健康状态
        }
    }

    /**
     * 记录失败请求
     */
    public void recordFailure() {
        this.totalRequests++;
        this.failedRequests++;
        this.lastUsedTime = LocalDateTime.now();
        this.lastFailTime = LocalDateTime.now();
        this.failCount++;

        // 连续失败超过5次，标记为不健康
        if (this.failCount >= 5) {
            this.healthStatus = HealthStatus.UNHEALTHY;
        }
    }

    /**
     * 手动禁用
     */
    public void disable() {
        this.healthStatus = HealthStatus.DISABLED;
        this.status = 0;
    }

    /**
     * 手动启用
     */
    public void enable() {
        this.healthStatus = HealthStatus.HEALTHY;
        this.status = 1;
        this.failCount = 0;
    }

    /**
     * 消费配额
     */
    public void consumeQuota(long amount) {
        if (quotaTotal != null && quotaTotal != -1) {
            this.quotaUsed += amount;
        }
    }
}
