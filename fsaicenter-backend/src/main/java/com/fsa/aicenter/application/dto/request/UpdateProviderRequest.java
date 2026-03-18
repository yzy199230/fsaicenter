package com.fsa.aicenter.application.dto.request;

import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新提供商请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "更新提供商请求")
public class UpdateProviderRequest {

    @NotBlank(message = "提供商名称不能为空")
    @Schema(description = "提供商名称", example = "OpenAI")
    private String name;

    @Schema(description = "基础URL", example = "https://api.openai.com/v1")
    private String baseUrl;

    @Schema(description = "协议类型", example = "openai_compatible")
    private String protocolType;

    @Schema(description = "Chat端点", example = "/chat/completions")
    private String chatEndpoint;

    @Schema(description = "Embedding端点", example = "/embeddings")
    private String embeddingEndpoint;

    @Schema(description = "Image端点", example = "/images/generations")
    private String imageEndpoint;

    @Schema(description = "Video端点", example = "/videos/generations")
    private String videoEndpoint;

    @Schema(description = "额外请求头(JSON)")
    private String extraHeaders;

    @Schema(description = "请求模板(JSON)")
    private String requestTemplate;

    @Schema(description = "响应映射(JSON)")
    private String responseMapping;

    @Schema(description = "认证类型", example = "bearer_key")
    private String authType;

    @Schema(description = "认证头名称", example = "Authorization")
    private String authHeader;

    @Schema(description = "认证前缀", example = "Bearer ")
    private String authPrefix;

    @Schema(description = "是否需要API Key", example = "true")
    private Boolean apiKeyRequired;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态", example = "ENABLED")
    private EntityStatus status;
}
