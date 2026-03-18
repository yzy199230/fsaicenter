package com.fsa.aicenter.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建模型API Key请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "创建模型API Key请求")
public class CreateModelApiKeyRequest {

    @Schema(description = "模型ID（由路径参数自动设置）", example = "1")
    private Long modelId;

    @NotBlank(message = "Key名称不能为空")
    @Schema(description = "Key名称", example = "主Key")
    private String keyName;

    @NotBlank(message = "API Key不能为空")
    @Schema(description = "API Key", example = "sk-xxxxxxxxxxxx")
    private String apiKey;

    @Min(value = 1, message = "权重必须大于0")
    @Schema(description = "权重（用于加权负载均衡）", example = "1")
    private Integer weight;

    @Schema(description = "每分钟请求限制", example = "60")
    private Integer rateLimitPerMinute;

    @Schema(description = "每天请求限制", example = "10000")
    private Integer rateLimitPerDay;

    @Schema(description = "总配额（-1表示无限制）", example = "-1")
    private Long quotaTotal;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "描述")
    private String description;
}
