package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI流式响应数��块
 * <p>
 * 表示流式响应中的单个数据块，用于SSE（Server-Sent Events）传输
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStreamChunk {
    /**
     * 响应ID
     * <p>同一次请求的所有chunk共享同一个ID</p>
     */
    private String id;

    /**
     * 增量内容
     * <p>本次chunk新增的内容，需累加到完整响应中</p>
     */
    private String delta;

    /**
     * 是否完成
     * <p>true表示这是最后一个chunk</p>
     */
    private boolean done;

    /**
     * 结束原因
     * <p>仅在done=true时有值，取值同AiResponse.finishReason</p>
     */
    private String finishReason;

    /**
     * 提示词消耗的token数
     * <p>仅在done=true时有值</p>
     */
    private Integer promptTokens;

    /**
     * 生成内容消耗的token数
     * <p>仅在done=true时有值</p>
     */
    private Integer completionTokens;

    /**
     * 总token数
     * <p>仅在done=true时有值</p>
     */
    private Integer totalTokens;

    /**
     * 模型标识
     */
    private String model;

    /**
     * 创建时间戳（Unix时间戳，秒）
     */
    private Long created;

    /**
     * 创建内容chunk
     *
     * @param id    响应ID
     * @param delta 增量内容
     * @return 内容chunk对象
     */
    public static AiStreamChunk content(String id, String delta) {
        return AiStreamChunk.builder()
                .id(id)
                .delta(delta)
                .done(false)
                .build();
    }

    /**
     * 创建完成chunk
     *
     * @param id            响应ID
     * @param finishReason  结束原因
     * @param promptTokens  提示词token数
     * @param completionTokens 生成内容token数
     * @return 完成chunk对象
     */
    public static AiStreamChunk done(String id, String finishReason,
                                     Integer promptTokens, Integer completionTokens) {
        Integer totalTokens = (promptTokens != null && completionTokens != null)
                ? promptTokens + completionTokens : null;

        return AiStreamChunk.builder()
                .id(id)
                .done(true)
                .finishReason(finishReason)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .build();
    }

    /**
     * 是否有内容
     *
     * @return true表示有增量内容
     */
    public boolean hasContent() {
        return delta != null && !delta.isEmpty();
    }

    /**
     * 计算总token数
     * <p>
     * 如果promptTokens和completionTokens都不为null，返回两者之和；
     * 否则返回totalTokens字段的值（可能为null）。
     * </p>
     *
     * @return 总token数，可能为null
     */
    public Integer calculateTotalTokens() {
        if (promptTokens == null || completionTokens == null) {
            return totalTokens;
        }
        return promptTokens + completionTokens;
    }
}
