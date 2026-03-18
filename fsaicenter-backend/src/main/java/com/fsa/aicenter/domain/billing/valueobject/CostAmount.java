package com.fsa.aicenter.domain.billing.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 成本金额值对象（不可变）
 */
public class CostAmount {
    private final BigDecimal amount;
    private final String currency;

    public CostAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative, but was: " + amount);
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        // 统一保留4位小数
        this.amount = amount.setScale(4, RoundingMode.HALF_UP);
        this.currency = currency.trim().toUpperCase();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    /**
     * 相加（相同货币）
     */
    public CostAmount add(CostAmount other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot add different currencies: " + this.currency + " and " + other.currency
            );
        }
        return new CostAmount(this.amount.add(other.amount), this.currency);
    }

    /**
     * 判断是否为零
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CostAmount)) return false;
        CostAmount that = (CostAmount) o;
        return amount.compareTo(that.amount) == 0 && currency.equals(that.currency);
    }

    @Override
    public int hashCode() {
        // 使用stripTrailingZeros()确保相同值有相同hashCode，与equals()一致
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
