package com.fsa.aicenter.interfaces.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddings请求参数
 * <p>
 * 提供文本向量化服务的请求对象，支持单个或批量文本的向量化。
 * </p>
 *
 * @author FSA AI Center
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingsRequest {
    /**
     * 模型代码（可选，指定具体模型）
     * <p>如果不指定，则根据modelType选择</p>
     */
    private String model;

    /**
     * 模型类型（必填，必须为embedding）
     */
    @NotBlank(message = "模型类型不能为空")
    @Pattern(regexp = "embedding", message = "模型类型必须为embedding")
    @JsonProperty("model_type")
    private String modelType;

    /**
     * 输入文本（必填）
     * <p>
     * 支持以下格式：
     * <ul>
     *   <li>单个文本：字符串</li>
     *   <li>多个文本：字符串数组</li>
     * </ul>
     * </p>
     */
    @NotNull(message = "输入文本不能为空")
    private Object input;

    /**
     * 编码格式（可选）
     * <p>
     * 支持的格式：
     * <ul>
     *   <li>float: 浮点数数组（默认）</li>
     *   <li>base64: Base64编码字符串</li>
     * </ul>
     * </p>
     */
    @JsonProperty("encoding_format")
    @Pattern(regexp = "float|base64", message = "编码格式只能是float或base64")
    private String encodingFormat;

    /**
     * 向量维度（可选）
     * <p>部分模型支持指定输出向量的维度</p>
     */
    private Integer dimensions;

    /**
     * 用户标识（可选）
     * <p>用于追踪和滥用检测</p>
     */
    private String user;

    /**
     * 获取编码格式，默认为float
     */
    public String getEncodingFormat() {
        return encodingFormat != null ? encodingFormat : "float";
    }
}
