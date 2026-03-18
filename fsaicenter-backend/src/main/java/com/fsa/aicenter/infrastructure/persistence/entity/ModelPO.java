package com.fsa.aicenter.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型持久化对象
 */
@Data
@TableName("ai_model")
public class ModelPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("provider_id")
    private Long providerId;

    @TableField("model_code")
    private String modelCode;

    @TableField("model_name")
    private String modelName;

    @TableField("model_type")
    private String modelType;

    @TableField("support_stream")
    private Boolean supportStream;

    @TableField("max_tokens")
    private Integer maxTokens;

    /**
     * 模型配置(存储为JSONB)
     */
    @TableField("config")
    private String config;

    @TableField("description")
    private String description;

    /**
     * 模型能力配置(存储为JSONB)
     */
    @TableField("capabilities")
    private String capabilities;

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
