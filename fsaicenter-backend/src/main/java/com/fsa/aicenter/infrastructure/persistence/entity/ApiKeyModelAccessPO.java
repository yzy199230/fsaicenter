package com.fsa.aicenter.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API密钥模型访问权限持久化对象
 */
@Data
@TableName("api_key_model_access")
public class ApiKeyModelAccessPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("api_key_id")
    private Long apiKeyId;

    @TableField("model_id")
    private Long modelId;

    /**
     * 访问类型 (1:允许 0:禁止)
     */
    @TableField("access_type")
    private Integer accessType;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
