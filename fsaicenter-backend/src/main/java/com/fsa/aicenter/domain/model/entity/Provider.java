package com.fsa.aicenter.domain.model.entity;

import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ProviderType;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 提供商实体
 */
@Data
@ToString
public class Provider {

    // 协议类型常量
    public static final String PROTOCOL_OPENAI_COMPATIBLE = "openai_compatible";
    public static final String PROTOCOL_OPENAI_LIKE = "openai_like";
    public static final String PROTOCOL_CUSTOM_HTTP = "custom_http";
    public static final String PROTOCOL_WEBSOCKET = "websocket";
    public static final String PROTOCOL_SDK_REQUIRED = "sdk_required";

    private Long id;
    private String code;
    private String name;
    private ProviderType type;
    private String baseUrl;
    private String protocolType;
    private String chatEndpoint;
    private String embeddingEndpoint;
    private String imageEndpoint;
    private String videoEndpoint;
    private String extraHeaders;
    private String requestTemplate;
    private String responseMapping;
    private String authType;
    private String authHeader;
    private String authPrefix;
    private Boolean apiKeyRequired;
    private String description;
    private Integer sortOrder;
    private EntityStatus status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return status != null && status.isEnabled();
    }

    /**
     * 是否远程提供商
     */
    public boolean isRemote() {
        return type == ProviderType.REMOTE;
    }

    /**
     * 启用
     */
    public void enable() {
        this.status = EntityStatus.ENABLED;
    }

    /**
     * 禁用
     */
    public void disable() {
        this.status = EntityStatus.DISABLED;
    }

    public boolean isOpenAiCompatible() {
        return PROTOCOL_OPENAI_COMPATIBLE.equals(protocolType) || PROTOCOL_OPENAI_LIKE.equals(protocolType);
    }

    public String getChatEndpointOrDefault() {
        return chatEndpoint != null ? chatEndpoint : "/chat/completions";
    }

    public String getEmbeddingEndpointOrDefault() {
        return embeddingEndpoint != null ? embeddingEndpoint : "/embeddings";
    }

    public String getImageEndpointOrDefault() {
        return imageEndpoint != null ? imageEndpoint : "/images/generations";
    }

    public String getVideoEndpointOrDefault() {
        return videoEndpoint != null ? videoEndpoint : "/videos/generations";
    }

    public String getAuthPrefixOrDefault() {
        return authPrefix != null ? authPrefix : "Bearer ";
    }
}
