package com.fsa.aicenter.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提供商响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "提供商响应")
public class ProviderResponse {

    @Schema(description = "提供商ID")
    private Long id;

    @Schema(description = "提供商编码")
    private String code;

    @Schema(description = "提供商名称")
    private String name;

    @Schema(description = "提供商类型")
    private ProviderType type;

    @Schema(description = "基础URL")
    private String baseUrl;

    @Schema(description = "协议类型")
    private String protocolType;

    @Schema(description = "Chat端点")
    private String chatEndpoint;

    @Schema(description = "Embedding端点")
    private String embeddingEndpoint;

    @Schema(description = "Image端点")
    private String imageEndpoint;

    @Schema(description = "Video端点")
    private String videoEndpoint;

    @Schema(description = "额外请求头(JSON)")
    private String extraHeaders;

    @Schema(description = "请求模板(JSON)")
    private String requestTemplate;

    @Schema(description = "响应映射(JSON)")
    private String responseMapping;

    @Schema(description = "认证类型")
    private String authType;

    @Schema(description = "认证头名称")
    private String authHeader;

    @Schema(description = "认证前缀")
    private String authPrefix;

    @Schema(description = "是否需要API Key")
    private Boolean apiKeyRequired;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态")
    private EntityStatus status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
