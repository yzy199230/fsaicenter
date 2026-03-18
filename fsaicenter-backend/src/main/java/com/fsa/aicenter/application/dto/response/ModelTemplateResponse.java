package com.fsa.aicenter.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 模型模板响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "模型模板响应")
public class ModelTemplateResponse {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "模板编码")
    private String code;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模型类型")
    private String type;

    @Schema(description = "提供商编码")
    private String providerCode;

    @Schema(description = "提供商名称")
    private String providerName;

    @Schema(description = "是否支持流式")
    private Boolean supportStream;

    @Schema(description = "最大Token限制")
    private Integer maxTokenLimit;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "能力配置")
    private Map<String, Object> capabilities;

    @Schema(description = "默认配置")
    private Map<String, Object> defaultConfig;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "是否已弃用")
    private Boolean deprecated;

    @Schema(description = "发布日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    @Schema(description = "模板来源")
    private String source;
}
