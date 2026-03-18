package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 图片生成请求参数
 * <p>
 * 参考OpenAI Images API规范
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequest {
    /**
     * 图片描述提示词
     */
    @NotBlank(message = "提示词不能为空")
    private String prompt;

    /**
     * 负面提示词
     * <p>描述不希望出现在图片中的内容</p>
     */
    private String negativePrompt;

    /**
     * 生成图片数量
     * <p>默认1，范围1-10</p>
     */
    @Builder.Default
    private Integer n = 1;

    /**
     * 图片尺寸
     * <p>如：256x256, 512x512, 1024x1024, 1792x1024, 1024x1792</p>
     */
    private String size;

    /**
     * 响应格式
     * <p>url 或 b64_json</p>
     */
    private String responseFormat;

    /**
     * 图片风格
     * <p>vivid（生动）或 natural（自然）</p>
     */
    private String style;

    /**
     * 图片质量
     * <p>standard（标准）或 hd（高清）</p>
     */
    private String quality;

    /**
     * 用户标识
     * <p>用于追踪和滥用检测</p>
     */
    private String user;
}