package com.fsa.aicenter.domain.billing.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 计费规则值对象（不可变）
 */
public class BillingRule {
    private final Long id;
    private final Long modelId;
    private final BillingType billingType;
    private final BigDecimal unitPrice;       // 单位价格（元）
    private final BigDecimal inputUnitPrice;  // 输入单价（元），仅 TOKEN 类型
    private final BigDecimal outputUnitPrice; // 输出单价（元），仅 TOKEN 类型
    private final Integer unitAmount;         // 计费单位数量
    private final String currency;
    private final LocalDateTime effectiveTime;
    private final LocalDateTime expireTime;
    private final String description;

    public BillingRule(Long id, Long modelId, BillingType billingType, BigDecimal unitPrice,
                      BigDecimal inputUnitPrice, BigDecimal outputUnitPrice,
                      Integer unitAmount, String currency,
                      LocalDateTime effectiveTime, LocalDateTime expireTime, String description) {
        this.id = id;
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }
        if (billingType == null) {
            throw new IllegalArgumentException("Billing type cannot be null");
        }
        // TOKEN 类型需要 inputUnitPrice 和 outputUnitPrice，其他类型需要 unitPrice
        if (billingType == BillingType.TOKEN) {
            if (inputUnitPrice == null || inputUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Input unit price must be >= 0 for TOKEN type");
            }
            if (outputUnitPrice == null || outputUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Output unit price must be >= 0 for TOKEN type");
            }
        } else {
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price must be >= 0, but was: " + unitPrice);
            }
        }
        if (unitAmount == null || unitAmount <= 0) {
            throw new IllegalArgumentException("Unit amount must be > 0, but was: " + unitAmount);
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (effectiveTime == null) {
            throw new IllegalArgumentException("Effective time cannot be null");
        }

        this.modelId = modelId;
        this.billingType = billingType;
        this.unitPrice = unitPrice != null ? unitPrice.setScale(6, RoundingMode.HALF_UP) : null;
        this.inputUnitPrice = inputUnitPrice != null ? inputUnitPrice.setScale(6, RoundingMode.HALF_UP) : null;
        this.outputUnitPrice = outputUnitPrice != null ? outputUnitPrice.setScale(6, RoundingMode.HALF_UP) : null;
        this.unitAmount = unitAmount;
        this.currency = currency.trim().toUpperCase();
        this.effectiveTime = effectiveTime;
        this.expireTime = expireTime;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getModelId() {
        return modelId;
    }

    public BillingType getBillingType() {
        return billingType;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getInputUnitPrice() {
        return inputUnitPrice;
    }

    public BigDecimal getOutputUnitPrice() {
        return outputUnitPrice;
    }

    public Integer getUnitAmount() {
        return unitAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getEffectiveTime() {
        return effectiveTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否有效（在有效期内）
     */
    public boolean isEffective(LocalDateTime now) {
        if (now == null) {
            now = LocalDateTime.now();
        }
        boolean afterEffective = !now.isBefore(effectiveTime);
        boolean beforeExpire = expireTime == null || now.isBefore(expireTime);
        return afterEffective && beforeExpire;
    }

    /**
     * 计算成本
     */
    public CostAmount calculate(UsageMetrics metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("Usage metrics cannot be null");
        }
        if (!this.billingType.equals(metrics.getBillingType())) {
            throw new IllegalArgumentException(
                "Billing type mismatch: rule=" + this.billingType + ", metrics=" + metrics.getBillingType()
            );
        }

        // 费用 = (使用量 / 单位数量) * 单位价格
        BigDecimal usage = new BigDecimal(metrics.getUsageAmount());
        BigDecimal unit = new BigDecimal(unitAmount);
        BigDecimal usagePerUnit = usage.divide(unit, 10, RoundingMode.HALF_UP);

        BigDecimal cost;
        if (billingType == BillingType.TOKEN && unitPrice == null) {
            // TOKEN类型使用输入/输出分别计价，这里简化为使用输入单价
            // 因为当前metrics只有总量，无法区分输入输出
            BigDecimal price = inputUnitPrice != null ? inputUnitPrice : BigDecimal.ZERO;
            cost = usagePerUnit.multiply(price);
        } else {
            cost = usagePerUnit.multiply(unitPrice != null ? unitPrice : BigDecimal.ZERO);
        }

        return new CostAmount(cost, currency);
    }

    /**
     * equals和hashCode基于业务标识：(modelId, billingType)
     * 注意：同一模型的不同时间段的计费规则会被认为相等
     * 这是业务设计决策，不同时间段的规则通过effectiveTime区分
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BillingRule)) return false;
        BillingRule that = (BillingRule) o;
        return modelId.equals(that.modelId) && billingType == that.billingType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, billingType);
    }

    @Override
    public String toString() {
        return "BillingRule{" +
               "modelId=" + modelId +
               ", billingType=" + billingType +
               ", unitPrice=" + unitPrice +
               ", inputUnitPrice=" + inputUnitPrice +
               ", outputUnitPrice=" + outputUnitPrice +
               ", unitAmount=" + unitAmount +
               ", currency='" + currency + '\'' +
               '}';
    }
}
