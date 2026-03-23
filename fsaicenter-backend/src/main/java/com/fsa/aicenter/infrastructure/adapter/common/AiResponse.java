package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI响应结果
 * <p>
 * 统一的AI调用响应对象，屏蔽不同提供商的响应差异
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponse {
    /**
     * 响应ID
     * <p>提供商返回的请求标识</p>
     */
    private String id;

    /**
     * AI生成的内容
     */
    private String content;

    /**
     * 提示词消耗的token数
     */
    private Integer promptTokens;

    /**
     * 生成内容消耗的token数
     */
    private Integer completionTokens;

    /**
     * 总token数
     * <p>promptTokens + completionTokens</p>
     */
    private Integer totalTokens;

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
     * 模型标识
     * <p>实际使用的模型（可能与请求不同）</p>
     */
    private String model;

    /**
     * 创建时间戳（Unix时间戳，秒）
     */
    private Long created;

    /**
     * 上游原始响应JSON（用于OpenAI兼容接口透传）
     */
    private String rawResponseBody;

    // ==================== 图片生成相关字段 ====================

    /**
     * 生成的图片数据列表
     * <p>用于Image Generation API</p>
     */
    private List<ImageData> images;

    /**
     * 图片数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageData {
        /**
         * 图片URL
         */
        private String url;

        /**
         * Base64编码的图片数据
         */
        private String b64Json;

        /**
         * 修改后的提示词
         * <p>AI可能会优化原始提示词</p>
         */
        private String revisedPrompt;
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

    /**
     * 是否正常结束
     *
     * @return true表示正常结束
     */
    public boolean isNormalFinish() {
        return FinishReason.STOP.getValue().equals(finishReason);
    }

    /**
     * 是否因长度限制结束
     *
     * @return true表示达到长度限制
     */
    public boolean isLengthFinish() {
        return FinishReason.LENGTH.getValue().equals(finishReason);
    }

    // ==================== 向量嵌入相关字段 ====================

    /**
     * 向量数据
     * <p>用于Embedding API</p>
     */
    private List<Double> embedding;

    /**
     * 生成的图片URL列表
     * <p>用于Image Generation API</p>
     */
    private List<String> imageUrls;

    // ==================== 音频相关字段 ====================

    /**
     * 音频URL
     * <p>用于TTS语音合成</p>
     */
    private String audioUrl;

    /**
     * 音频Base64数据
     */
    private String audioBase64;

    /**
     * 音频时长（秒）
     */
    private Double audioDuration;

    // ==================== 视频相关字段 ====================

    /**
     * 视频URL
     * <p>用于视频生成</p>
     */
    private String videoUrl;

    /**
     * 视频URL列表
     * <p>用于视频生成API，支持生成多个视频</p>
     */
    private List<String> videoUrls;

    /**
     * 视频时长（秒）
     */
    private Double videoDuration;
}
