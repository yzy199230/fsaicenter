package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量嵌入响应结果
 * <p>
 * 参考OpenAI Embedding API规范
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {
    /**
     * 响应ID
     */
    private String id;

    /**
     * 模型标识
     */
    private String model;

    /**
     * 向量数据列表
     */
    private List<EmbeddingData> data;

    /**
     * Token使用情况
     */
    private Usage usage;

    /**
     * 向量数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingData {
        /**
         * 索引（对应input列表中的位置）
         */
        private Integer index;

        /**
         * 向量数据（浮点数数组）
         */
        private List<Float> embedding;

        /**
         * Base64编码的向量（当encodingFormat为base64时）
         */
        private String embeddingBase64;
    }

    /**
     * Token使用情况
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        /**
         * 提示词token数
         */
        private Integer promptTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;
    }
}