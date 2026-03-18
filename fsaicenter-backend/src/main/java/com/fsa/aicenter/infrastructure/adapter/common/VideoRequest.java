package com.fsa.aicenter.infrastructure.adapter.common;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Video Generation请求参数
 * <p>
 * 参考OpenAI Video API规范
 * </p>
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoRequest {
    /**
     * 模型代码
     */
    @NotBlank(message = "模型标识不能为空")
    private String model;

    /**
     * 视频描述提示词
     * <p>描述想要生成的视频内容</p>
     */
    @NotBlank(message = "提示词不能为空")
    private String prompt;

    /**
     * 参考图片URL（可选）
     * <p>用于图片生成视频</p>
     */
    private String image;

    /**
     * 生成视频数量（可选，默认1）
     */
    private Integer n;

    /**
     * 视频时长（可选，秒）
     * <p>支持: 5, 10, 15, 20</p>
     */
    private Integer duration;

    /**
     * 视频分辨率（可选）
     * <p>支持: 256x256, 512x512, 1024x1024</p>
     */
    private String size;

    /**
     * 视频宽高比（可选）
     * <p>支持: 16:9, 9:16, 1:1</p>
     */
    private String aspectRatio;

    /**
     * 响应格式（可选，默认url）
     * <p>
     * <ul>
     *   <li>url: 返回视频URL</li>
     *   <li>b64_json: 返回Base64编码的视频</li>
     * </ul>
     * </p>
     */
    private String responseFormat;

    /**
     * 用户标识（可选）
     * <p>用于追踪和滥用检测</p>
     */
    private String user;

    /**
     * 获取n值，默认为1
     */
    public Integer getN() {
        return n != null ? n : 1;
    }

    /**
     * 获取responseFormat值，默认为url
     */
    public String getResponseFormat() {
        return responseFormat != null ? responseFormat : "url";
    }
}