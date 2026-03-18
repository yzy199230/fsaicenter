package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Video Generation响应结果
 * <p>
 * 参考OpenAI Video API规范
 * </p>
 *
 * @author FSA AI Center
 */
@Data
@Builder
public class VideoResponse {
    /**
     * 响应ID
     */
    private String id;

    /**
     * 使用的模型代码
     */
    private String model;

    /**
     * 创建时间戳（Unix时间戳，秒）
     */
    private Long created;

    /**
     * 生成的视频数据列表
     */
    private List<VideoData> data;

    /**
     * 视频数据项
     */
    @Data
    @Builder
    public static class VideoData {
        /**
         * 视频URL
         */
        private String url;

        /**
         * Base64编码的视频数据
         */
        private String b64Json;

        /**
         * 修订后的提示词（某些API会返回）
         */
        private String revisedPrompt;

        /**
         * 视频时长（秒）
         */
        private Integer duration;

        /**
         * 视频分辨率
         */
        private String size;

        /**
         * 视频宽高比
         */
        private String aspectRatio;
    }
}