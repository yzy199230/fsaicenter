package com.fsa.aicenter.domain.apikey.valueobject;

/**
 * 配额值对象（不可变）
 */
public class Quota {
    private final Long total;  // -1表示无限制
    private final Long used;

    public Quota(Long total, Long used) {
        this.total = total;
        this.used = used;
        validate();
    }

    public Long getTotal() {
        return total;
    }

    public Long getUsed() {
        return used;
    }

    /**
     * 是否无限制
     */
    public boolean isUnlimited() {
        return total != null && total == -1;
    }

    /**
     * 是否有足够配额
     */
    public boolean hasEnough(long requiredAmount) {
        if (isUnlimited()) {
            return true;
        }
        return remaining() >= requiredAmount;
    }

    /**
     * 剩余配额
     */
    public long remaining() {
        if (isUnlimited()) {
            return Long.MAX_VALUE;
        }
        if (total == null || used == null) {
            return 0;
        }
        return Math.max(0, total - used);
    }

    /**
     * 消费配额（返回新的Quota对象）
     */
    public Quota consume(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Consume amount cannot be negative, but was: " + amount);
        }
        if (isUnlimited()) {
            return this;
        }
        long newUsed = (used != null ? used : 0) + amount;
        return new Quota(total, newUsed);
    }

    /**
     * 退回配额（返回新的Quota对象）
     */
    public Quota refund(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Refund amount cannot be negative, but was: " + amount);
        }
        if (isUnlimited()) {
            return this;
        }
        long newUsed = Math.max(0, (used != null ? used : 0) - amount);
        return new Quota(total, newUsed);
    }

    /**
     * 校验配额值有效性
     */
    private void validate() {
        if (total != null && total != -1 && total < 0) {
            throw new IllegalArgumentException("Quota total must be -1 or >= 0, but was: " + total);
        }
        if (used != null && used < 0) {
            throw new IllegalArgumentException("Quota used must be >= 0, but was: " + used);
        }
        if (!isUnlimited() && total != null && used != null && used > total) {
            throw new IllegalStateException("Quota used (" + used + ") cannot exceed total (" + total + ")");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quota quota = (Quota) o;
        return java.util.Objects.equals(total, quota.total) &&
               java.util.Objects.equals(used, quota.used);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(total, used);
    }

    @Override
    public String toString() {
        return "Quota(total=" + total + ", used=" + used + ")";
    }
}
