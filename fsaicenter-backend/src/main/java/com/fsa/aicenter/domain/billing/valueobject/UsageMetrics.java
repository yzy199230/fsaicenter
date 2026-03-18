package com.fsa.aicenter.domain.billing.valueobject;

import java.util.Objects;

/**
 * 使用指标值对象（不可变）
 *
 * 注意：usageAmount的单位取决于billingType：
 * - TOKEN: Token数量
 * - IMAGE: 图片数量
 * - AUDIO_DURATION: 音频时长（秒）
 */
public class UsageMetrics {
    private final BillingType billingType;
    private final Long usageAmount;  // 使用量，单位由billingType决定

    public UsageMetrics(BillingType billingType, Long usageAmount) {
        if (billingType == null) {
            throw new IllegalArgumentException("Billing type cannot be null");
        }
        if (usageAmount == null) {
            throw new IllegalArgumentException("Usage amount cannot be null");
        }
        if (usageAmount < 0) {
            throw new IllegalArgumentException("Usage amount cannot be negative, but was: " + usageAmount);
        }
        this.billingType = billingType;
        this.usageAmount = usageAmount;
    }

    public BillingType getBillingType() {
        return billingType;
    }

    public Long getUsageAmount() {
        return usageAmount;
    }

    /**
     * 使用量是否为零
     */
    public boolean isZero() {
        return usageAmount == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsageMetrics)) return false;
        UsageMetrics that = (UsageMetrics) o;
        return billingType == that.billingType && usageAmount.equals(that.usageAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billingType, usageAmount);
    }

    @Override
    public String toString() {
        return usageAmount + " " + billingType.getCode();
    }
}
