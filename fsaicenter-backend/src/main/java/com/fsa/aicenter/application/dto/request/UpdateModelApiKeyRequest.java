package com.fsa.aicenter.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新模型API Key请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "更新模型API Key请求")
public class UpdateModelApiKeyRequest {

    @Schema(description = "Key名称", example = "主Key")
    private String keyName;

    @Schema(description = "API Key（不填则不修改）", example = "sk-xxxxxxxxxxxx")
    private String apiKey;

    @Min(value = 1, message = "权重必须大于0")
    @Schema(description = "权重", example = "1")
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

    @Schema(description = "状态")
    private EntityStatus status;
}
