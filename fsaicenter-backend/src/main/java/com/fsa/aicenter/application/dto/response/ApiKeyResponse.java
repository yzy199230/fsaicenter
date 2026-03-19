package com.fsa.aicenter.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * API密钥响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "API密钥响应")
public class ApiKeyResponse {

    @Schema(description = "密钥ID")
    private Long id;

    @Schema(description = "密钥值")
    private String keyValue;

    @Schema(description = "密钥名称")
    private String keyName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "配额总量")
    private Long quotaTotal;

    @Schema(description = "已使用配额")
    private Long quotaUsed;

    @Schema(description = "剩余配额")
    private Long quotaRemaining;

    @Schema(description = "每分钟限流次数")
    private Integer rateLimitPerMinute;

    @Schema(description = "每天限流次数")
    private Integer rateLimitPerDay;

    @Schema(description = "允许访问的模型类型")
    private Set<ModelType> allowedModelTypes;

    @Schema(description = "IP白名单")
    private Set<String> allowedIpWhitelist;

    @Schema(description = "允许访问的模型ID列表")
    private List<Long> allowedModelIds;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description = "状态")
    private EntityStatus status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
