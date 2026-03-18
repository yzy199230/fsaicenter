package com.fsa.aicenter.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 保存用户模板请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "保存用户模板请求")
public class SaveUserTemplateRequest {

    @NotBlank(message = "模板代码不能为空")
    @Schema(description = "模板代码", example = "custom-model-1")
    private String code;

    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称", example = "自定义模型1")
    private String name;

    @NotBlank(message = "模型类型不能为空")
    @Schema(description = "模型类型", example = "CHAT")
    private String type;

    @NotBlank(message = "提供商代码不能为空")
    @Schema(description = "提供商代码", example = "openai")
    private String providerCode;

    @Schema(description = "是否支持流式", example = "true")
    private Boolean supportStream;

    @Schema(description = "最大Token限制", example = "8192")
    private Integer maxTokenLimit;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "能力配置")
    private Map<String, Object> capabilities;

    @Schema(description = "默认配置")
    private Map<String, Object> defaultConfig;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "是否已弃用", example = "false")
    private Boolean deprecated;

    @Schema(description = "发布日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;
}
