package com.fsa.aicenter.interfaces.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Image Generation请求参数
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequest {
    /**
     * 模型代码（可选，指定具体模型）
     * <p>如果不指定，则根据modelType选择</p>
     */
    private String model;

    /**
     * 模型类型（必填，必须为"image"）
     */
    @NotBlank(message = "模型类型不能为空")
    @Pattern(regexp = "image", message = "模型类型必须为image")
    private String modelType;

    /**
     * 图片描述提示词（必填）
     * <p>描述想要生成的图片内容</p>
     */
    @NotBlank(message = "图片描述不能为空")
    private String prompt;

    /**
     * 负面提示词（可选）
     * <p>描述不希望出现在图片中的内容</p>
     */
    private String negativePrompt;

    /**
     * 生成图片数量（可选，默认1）
     * <p>范围: 1-10</p>
     */
    @Min(value = 1, message = "图片数量至少为1")
    @Max(value = 10, message = "图片数量最多为10")
    private Integer n;

    /**
     * 图片尺寸（可选）
     * <p>
     * 支持的尺寸:
     * <ul>
     *   <li>256x256</li>
     *   <li>512x512</li>
     *   <li>1024x1024</li>
     *   <li>1024x1792 (竖图)</li>
     *   <li>1792x1024 (横图)</li>
     * </ul>
     * </p>
     */
    @Pattern(
        regexp = "^(256x256|512x512|1024x1024|1024x1792|1792x1024)?$",
        message = "图片尺寸必须为: 256x256, 512x512, 1024x1024, 1024x1792, 1792x1024"
    )
    private String size;

    /**
     * 响应格式（可选，默认url）
     * <p>
     * <ul>
     *   <li>url: 返回图片URL</li>
     *   <li>b64_json: 返回Base64编码的图片</li>
     * </ul>
     * </p>
     */
    @Pattern(
        regexp = "^(url|b64_json)?$",
        message = "响应格式必须为url或b64_json"
    )
    private String responseFormat;

    /**
     * 图片风格（可选）
     * <p>
     * <ul>
     *   <li>vivid: 生动、鲜艳的风格</li>
     *   <li>natural: 自然、真实的风格</li>
     * </ul>
     * </p>
     */
    @Pattern(
        regexp = "^(vivid|natural)?$",
        message = "图片风格必须为vivid或natural"
    )
    private String style;

    /**
     * 图片质量（可选）
     * <p>
     * <ul>
     *   <li>standard: 标准质量</li>
     *   <li>hd: 高清质量</li>
     * </ul>
     * </p>
     */
    @Pattern(
        regexp = "^(standard|hd)?$",
        message = "图片质量必须为standard或hd"
    )
    private String quality;

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
