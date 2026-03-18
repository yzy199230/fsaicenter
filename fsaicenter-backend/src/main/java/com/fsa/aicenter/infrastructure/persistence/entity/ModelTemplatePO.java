package com.fsa.aicenter.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fsa.aicenter.infrastructure.persistence.handler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模型模板持久化对象
 */
@Data
@TableName(value = "model_template", autoResultMap = true)
public class ModelTemplatePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("provider_code")
    private String providerCode;

    @TableField("support_stream")
    private Boolean supportStream;

    @TableField("max_token_limit")
    private Integer maxTokenLimit;

    @TableField("description")
    private String description;

    /**
     * 能力配置(存储为JSONB)
     */
    @TableField(value = "capabilities", typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> capabilities;

    /**
     * 默认配置(存储为JSONB)
     */
    @TableField(value = "default_config", typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> defaultConfig;

    /**
     * 标签列表(存储为PostgreSQL数组)
     */
    @TableField(value = "tags", typeHandler = com.fsa.aicenter.infrastructure.persistence.handler.StringListTypeHandler.class)
    private List<String> tags;

    @TableField("deprecated")
    private Boolean deprecated;

    @TableField("release_date")
    private LocalDate releaseDate;

    @TableField("source")
    private String source;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
