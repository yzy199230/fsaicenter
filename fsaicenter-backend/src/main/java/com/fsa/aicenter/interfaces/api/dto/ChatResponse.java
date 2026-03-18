package com.fsa.aicenter.interfaces.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat Completion响应结果
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /**
     * 响应ID
     */
    private String id;

    /**
     * 使用的模型代码
     */
    private String model;

    /**
     * AI生成的回复内容
     */
    private String content;

    /**
     * Token使用情况
     */
    private Usage usage;

    /**
     * 结束原因
     * <ul>
     *   <li>stop: 正常结束</li>
     *   <li>length: 达到最大token限制</li>
     *   <li>content_filter: 内容过滤</li>
     *   <li>tool_calls: 调用工具</li>
     * </ul>
     */
    private String finishReason;

    /**
     * 创建时间戳（Unix时间戳，秒）
     */
    private Long created;
}
