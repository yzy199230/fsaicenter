package com.fsa.aicenter.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型API Key持久化对象
 */
@Data
@TableName("model_api_key")
public class ModelApiKeyPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("model_id")
    private Long modelId;

    @TableField("key_name")
    private String keyName;

    @TableField(value = "api_key", typeHandler = com.fsa.aicenter.infrastructure.persistence.typehandler.EncryptedApiKeyTypeHandler.class)
    private String apiKey;

    @TableField("weight")
    private Integer weight;

    @TableField("total_requests")
    private Long totalRequests;

    @TableField("success_requests")
    private Long successRequests;

    @TableField("failed_requests")
    private Long failedRequests;

    @TableField("last_used_time")
    private LocalDateTime lastUsedTime;

    @TableField("last_success_time")
    private LocalDateTime lastSuccessTime;

    @TableField("last_fail_time")
    private LocalDateTime lastFailTime;

    @TableField("health_status")
    private Integer healthStatus;

    @TableField("fail_count")
    private Integer failCount;

    @TableField("rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    @TableField("rate_limit_per_day")
    private Integer rateLimitPerDay;

    @TableField("quota_total")
    private Long quotaTotal;

    @TableField("quota_used")
    private Long quotaUsed;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("status")
    private Integer status;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("description")
    private String description;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
