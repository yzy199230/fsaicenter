package com.fsa.aicenter.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fsa.aicenter.domain.model.valueobject.HealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型API Key响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "模型API Key响应")
public class ModelApiKeyResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "Key名称")
    private String keyName;

    @Schema(description = "API Key（完整，用于复制）")
    private String apiKey;

    @Schema(description = "API Key（脱敏显示）")
    private String apiKeyMasked;

    @Schema(description = "权重")
    private Integer weight;

    @Schema(description = "总请求数")
    private Long totalRequests;

    @Schema(description = "成功请求数")
    private Long successRequests;

    @Schema(description = "失败请求数")
    private Long failedRequests;

    @Schema(description = "最后使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedTime;

    @Schema(description = "最后成功时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSuccessTime;

    @Schema(description = "最后失败时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFailTime;

    @Schema(description = "健康状态")
    private HealthStatus healthStatus;

    @Schema(description = "连续失败次数")
    private Integer failCount;

    @Schema(description = "每分钟请求限制")
    private Integer rateLimitPerMinute;

    @Schema(description = "每天请求限制")
    private Integer rateLimitPerDay;

    @Schema(description = "总配额")
    private Long quotaTotal;

    @Schema(description = "已使用配额")
    private Long quotaUsed;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
