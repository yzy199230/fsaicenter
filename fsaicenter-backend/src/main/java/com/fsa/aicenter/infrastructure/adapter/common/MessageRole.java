package com.fsa.aicenter.infrastructure.adapter.common;

/**
 * AI消息角色枚举
 * <p>
 * 定义AI对话中的消息角色类型
 * </p>
 */
public enum MessageRole {
    /**
     * 系统消息
     * <p>用于设置AI的行为和上下文</p>
     */
    SYSTEM("system"),

    /**
     * 用户消息
     * <p>来自用户的输入内容</p>
     */
    USER("user"),

    /**
     * 助手消息
     * <p>AI助手的回复内容</p>
     */
    ASSISTANT("assistant");

    private final String value;

    MessageRole(String value) {
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
    public static MessageRole fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (MessageRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }
}
