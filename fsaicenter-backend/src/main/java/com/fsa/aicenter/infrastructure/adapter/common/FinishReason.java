package com.fsa.aicenter.infrastructure.adapter.common;

/**
 * AI响应完成原因枚举
 * <p>
 * 定义AI生成内容结束的各种原因
 * </p>
 */
public enum FinishReason {
    /**
     * 正常完成
     * <p>模型自然完成了内容生成</p>
     */
    STOP("stop"),

    /**
     * 达到最大长度限制
     * <p>生成的token数达到了maxTokens限制</p>
     */
    LENGTH("length"),

    /**
     * 内容过滤
     * <p>内容被安全过滤器拦截</p>
     */
    CONTENT_FILTER("content_filter"),

    /**
     * 工具调用
     * <p>模型请求调用外部工具/函数</p>
     */
    TOOL_CALLS("tool_calls");

    private final String value;

    FinishReason(String value) {
        this.value = value;
    }

    /**
     * 获取字符串值
     *
     * @return 枚举对应的字符串值
     */
    public String getValue() {
        return value;
    }

    /**
     * 从字符串值获取枚举
     *
     * @param value 字符串值
     * @return 对应的枚举，如果未找到返回null
     */
    public static FinishReason fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (FinishReason reason : values()) {
            if (reason.value.equals(value)) {
                return reason;
            }
        }
        return null;
    }
}
