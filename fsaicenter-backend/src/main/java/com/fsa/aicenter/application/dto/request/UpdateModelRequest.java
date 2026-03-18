package com.fsa.aicenter.application.dto.request;

import com.fsa.aicenter.domain.model.valueobject.ModelConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 更新模型请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "更新模型请求")
public class UpdateModelRequest {

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", example = "GPT-4")
    private String name;

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
