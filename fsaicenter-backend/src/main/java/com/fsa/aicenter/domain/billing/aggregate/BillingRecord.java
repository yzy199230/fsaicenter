package com.fsa.aicenter.domain.billing.aggregate;

import com.fsa.aicenter.domain.billing.valueobject.BillingRule;
import com.fsa.aicenter.domain.billing.valueobject.BillingType;
import com.fsa.aicenter.domain.billing.valueobject.CostAmount;
import com.fsa.aicenter.domain.billing.valueobject.UsageMetrics;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计费记录聚合根
 */
@Getter
@Setter
public class BillingRecord {
    private Long id;
    private String requestId;
    private Long apiKeyId;
    private Long modelId;
    private BillingType billingType;
    private Long usageAmount;
    private BigDecimal unitPrice;
    private CostAmount totalCost;
    private LocalDateTime billingTime;
    private LocalDateTime createdTime;

    /**
     * 无参构造器，仅供持久化框架使用
     */
    public BillingRecord() {
        // MyBatis等ORM框架需要无参构造器
    }

    // ========== 领域行为 ==========

    /**
     * 根据计费规则和使用指标计算成本（静态工厂方法）
     */
    public static BillingRecord create(String requestId, Long apiKeyId,
                                      BillingRule rule, UsageMetrics metrics) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        if (apiKeyId == null) {
            throw new IllegalArgumentException("API key ID cannot be null");
        }
        if (rule == null) {
            throw new IllegalArgumentException("Billing rule cannot be null");
        }
        if (metrics == null) {
            throw new IllegalArgumentException("Usage metrics cannot be null");
        }

        // 计算成本
        CostAmount cost = rule.calculate(metrics);

        BillingRecord record = new BillingRecord();
        record.requestId = requestId;
        record.apiKeyId = apiKeyId;
        record.modelId = rule.getModelId();
        record.billingType = metrics.getBillingType();
        record.usageAmount = metrics.getUsageAmount();
        record.unitPrice = rule.getUnitPrice();
        record.totalCost = cost;
        record.billingTime = LocalDateTime.now();
        record.createdTime = LocalDateTime.now();

        return record;
    }

    /**
     * 是否免费
     */
    public boolean isFree() {
        return totalCost != null && totalCost.isZero();
    }
}
