package com.fsa.aicenter.interfaces.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat Completion流式响应数据块
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamChunk {
    /**
     * 响应ID
     */
    private String id;

    /**
     * 使用的模型代码
     */
    private String model;

    /**
     * 增量内容
     */
    private String delta;

    /**
     * 是否完成
     */
    private boolean done;

    /**
     * 结束原因（仅在done=true时有值）
     */
    private String finishReason;

    /**
     * Token使用情况（仅在done=true时有值）
     */
    private Usage usage;

    /**
     * 创建时间戳（Unix时间戳，秒）
     */
    private Long created;
}
