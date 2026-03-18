package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * AI对话消息
 * <p>
 * 表示AI对话中的单条消息，遵循OpenAI标准格式
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    /**
     * 消息角色
     * <ul>
     *   <li>system: 系统提示词</li>
     *   <li>user: 用户消息</li>
     *   <li>assistant: AI助手回复</li>
     * </ul>
     */
    @NotBlank(message = "消息角色不能为空")
    private String role;

    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 创建系统消息
     *
     * @param content 系统提示词内容
     * @return 系统消息对象
     * @throws IllegalArgumentException 如果content为null或空白
     */
    public static Message system(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        return new Message(MessageRole.SYSTEM.getValue(), content);
    }

    /**
     * 创建用户消息
     *
     * @param content 用户消息内容
     * @return 用户消息对象
     * @throws IllegalArgumentException 如果content为null或空白
     */
    public static Message user(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        return new Message(MessageRole.USER.getValue(), content);
    }

    /**
     * 创建助手消息
     *
     * @param content 助手回复内容
     * @return 助手消息对象
     * @throws IllegalArgumentException 如果content为null或空白
     */
    public static Message assistant(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        return new Message(MessageRole.ASSISTANT.getValue(), content);
    }
}
