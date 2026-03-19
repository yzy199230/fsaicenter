package com.fsa.aicenter.application.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 创建API密钥请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "创建API密钥请求")
public class CreateApiKeyRequest {

    @NotBlank(message = "密钥名称不能为空")
    @Schema(description = "密钥名称", example = "业务系统A")
    private String keyName;

    @Schema(description = "描述")
    private String description;

    @NotNull(message = "配额总量不能为空")
    @Schema(description = "配额总量（-1表示无限制）", example = "1000000")
    private Long quotaTotal;

    @Schema(description = "每分钟限流次数", example = "60")
    private Integer rateLimitPerMinute;

    @Schema(description = "每天限流次数", example = "10000")
    private Integer rateLimitPerDay;

    @Schema(description = "允许访问的模型类型")
    private Set<ModelType> allowedModelTypes;

    @Schema(description = "IP白名单")
    private Set<String> allowedIpWhitelist;

    @Schema(description = "允许访问的模型ID列表（空表示不限制）")
    private Set<Long> allowedModelIds;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
