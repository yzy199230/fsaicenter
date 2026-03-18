package com.fsa.aicenter.application.dto.request;

import com.fsa.aicenter.domain.model.valueobject.ModelConfig;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 创建模型请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "创建模型请求")
public class CreateModelRequest {

    @NotBlank(message = "模型编码不能为空")
    @Schema(description = "模型编码", example = "gpt-4")
    private String code;

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", example = "GPT-4")
    private String name;

    @NotNull(message = "模型类型不能为空")
    @Schema(description = "模型类型", example = "CHAT")
    private ModelType type;

    @NotNull(message = "提供商ID不能为空")
    @Schema(description = "提供商ID", example = "1")
    private Long providerId;

    @Schema(description = "模型配置")
    private ModelConfig config;

    @Schema(description = "是否支持流式", example = "true")
    private Boolean supportStream;

    @Schema(description = "最大Token限制", example = "8192")
    private Integer maxTokenLimit;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "模型能力配置")
    private Map<String, Object> capabilities;
}
