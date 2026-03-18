package com.fsa.aicenter.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提供商持久化对象
 */
@Data
@TableName("ai_provider")
public class ProviderPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("provider_code")
    private String providerCode;

    @TableField("provider_name")
    private String providerName;

    @TableField("provider_type")
    private String providerType;

    @TableField("base_url")
    private String baseUrl;

    @TableField("protocol_type")
    private String protocolType;

    @TableField("chat_endpoint")
    private String chatEndpoint;

    @TableField("embedding_endpoint")
    private String embeddingEndpoint;

    @TableField("image_endpoint")
    private String imageEndpoint;

    @TableField("video_endpoint")
    private String videoEndpoint;

    @TableField("extra_headers")
    private String extraHeaders;

    @TableField("request_template")
    private String requestTemplate;

    @TableField("response_mapping")
    private String responseMapping;

    @TableField("auth_type")
    private String authType;

    @TableField("auth_header")
    private String authHeader;

    @TableField("auth_prefix")
    private String authPrefix;

    @TableField("api_key_required")
    private Boolean apiKeyRequired;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
