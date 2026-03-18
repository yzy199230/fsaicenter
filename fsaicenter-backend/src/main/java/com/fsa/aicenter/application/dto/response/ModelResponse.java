package com.fsa.aicenter.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelConfig;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 模型响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "模型响应")
public class ModelResponse {

    @Schema(description = "模型ID")
    private Long id;

    @Schema(description = "模型编码")
    private String code;

    @Schema(description = "模型名称")
    private String name;

    @Schema(description = "模型类型")
    private ModelType type;

    @Schema(description = "提供商ID")
    private Long providerId;

    @Schema(description = "提供商名称")
    private String providerName;

    @Schema(description = "模型配置")
    private ModelConfig config;

    @Schema(description = "是否支持流式")
    private Boolean supportStream;

    @Schema(description = "最大Token限制")
    private Integer maxTokenLimit;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态")
    private EntityStatus status;

    @Schema(description = "模型能力配置")
    private Map<String, Object> capabilities;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
