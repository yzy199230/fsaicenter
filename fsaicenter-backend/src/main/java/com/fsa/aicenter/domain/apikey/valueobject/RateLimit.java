package com.fsa.aicenter.domain.apikey.valueobject;

/**
 * 限流配置值对象（不可变）
 */
public class RateLimit {
    private final Integer perMinute;
    private final Integer perDay;

    public RateLimit(Integer perMinute, Integer perDay) {
        this.perMinute = perMinute;
        this.perDay = perDay;
        validate();
    }

    public Integer getPerMinute() {
        return perMinute;
    }

    public Integer getPerDay() {
        return perDay;
    }

    /**
     * 校验限流配置有效性
     */
    private void validate() {
        if (perMinute != null && perMinute < 0) {
            throw new IllegalArgumentException("Rate limit per minute must be >= 0, but was: " + perMinute);
        }
        if (perDay != null && perDay < 0) {
            throw new IllegalArgumentException("Rate limit per day must be >= 0, but was: " + perDay);
        }
    }

    /**
     * 是否有分钟级限流
     */
    public boolean hasMinuteLimit() {
        return perMinute != null && perMinute > 0;
    }

    /**
     * 是否有天级限流
     */
    public boolean hasDayLimit() {
        return perDay != null && perDay > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateLimit rateLimit = (RateLimit) o;
        return java.util.Objects.equals(perMinute, rateLimit.perMinute) &&
               java.util.Objects.equals(perDay, rateLimit.perDay);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(perMinute, perDay);
    }

    @Override
    public String toString() {
        return "RateLimit(perMinute=" + perMinute + ", perDay=" + perDay + ")";
    }
}
