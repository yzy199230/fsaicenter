package com.fsa.aicenter.domain.log.valueobject;

import java.util.Objects;

/**
 * Token使用量值对象（不可变）
 */
public class TokenUsage {
    private final Integer promptTokens;      // 输入Token数
    private final Integer completionTokens;  // 输出Token数
    private final Integer totalTokens;       // 总Token数

    public TokenUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        this.promptTokens = promptTokens != null ? promptTokens : 0;
        this.completionTokens = completionTokens != null ? completionTokens : 0;
        // 如果提供了totalTokens，使用提供的值；否则计算
        if (totalTokens != null) {
            this.totalTokens = totalTokens;
        } else {
            this.totalTokens = this.promptTokens + this.completionTokens;
        }
        validate();
    }

    /**
     * 便捷构造器：自动计算总Token数
     */
    public TokenUsage(Integer promptTokens, Integer completionTokens) {
        this(promptTokens, completionTokens, null);
    }

    /**
     * 零Token构造器
     */
    public static TokenUsage zero() {
        return new TokenUsage(0, 0, 0);
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    /**
     * Token总数允许的误差范围
     * 某些AI提供商返回的totalTokens可能与prompt+completion略有差异
     */
    private static final int TOTAL_TOKENS_TOLERANCE = 10;

    private void validate() {
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException(
                "Token counts cannot be negative: prompt=" + promptTokens +
                ", completion=" + completionTokens + ", total=" + totalTokens
            );
        }
        // 允许totalTokens略有误差（某些API可能不精确）
        int calculated = promptTokens + completionTokens;
        if (Math.abs(totalTokens - calculated) > TOTAL_TOKENS_TOLERANCE && calculated > 0) {
            throw new IllegalArgumentException(
                "Total tokens mismatch: provided=" + totalTokens + ", calculated=" + calculated
            );
        }
    }

    /**
     * 是否为零
     */
    public boolean isZero() {
        return totalTokens == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenUsage)) return false;
        TokenUsage that = (TokenUsage) o;
        return Objects.equals(promptTokens, that.promptTokens) &&
               Objects.equals(completionTokens, that.completionTokens) &&
               Objects.equals(totalTokens, that.totalTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promptTokens, completionTokens, totalTokens);
    }

    @Override
    public String toString() {
        return "TokenUsage{" +
               "prompt=" + promptTokens +
               ", completion=" + completionTokens +
               ", total=" + totalTokens +
               '}';
    }
}
