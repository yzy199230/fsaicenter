package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图片生成响应结果
 * <p>
 * 参考OpenAI Images API规范
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {
    /**
     * 创建时间戳（Unix时间戳，秒）
     */
    private Long created;

    /**
     * 生成的图片数据列表
     */
    private List<ImageData> data;

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
}