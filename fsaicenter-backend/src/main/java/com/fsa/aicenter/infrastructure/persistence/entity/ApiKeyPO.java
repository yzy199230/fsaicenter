package com.fsa.aicenter.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API密钥持久化对象
 */
@Data
@TableName("api_key")
public class ApiKeyPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("key_value")
    private String keyValue;

    @TableField("key_name")
    private String keyName;

    @TableField("description")
    private String description;

    /**
     * 总配额 (-1表示无限制)
     */
    @TableField("quota_total")
    private Long quotaTotal;

    /**
     * 已使用配额
     */
    @TableField("quota_used")
    private Long quotaUsed;

    /**
     * 每分钟限流
     */
    @TableField("rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    /**
     * 每天限流
     */
    @TableField("rate_limit_per_day")
    private Integer rateLimitPerDay;

    /**
     * 允许的模型类型(逗号分隔，如: CHAT,EMBEDDING)
     */
    @TableField("allowed_model_types")
    private String allowedModelTypes;

    /**
     * IP白名单(逗号分隔)
     */
    @TableField("allowed_ip_whitelist")
    private String allowedIpWhitelist;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("status")
    private Integer status;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
