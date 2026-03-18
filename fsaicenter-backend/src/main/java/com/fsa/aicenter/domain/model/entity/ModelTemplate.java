package com.fsa.aicenter.domain.model.entity;

import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.domain.model.valueobject.TemplateSource;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模型模板实体
 * 用于定义标准模型模板库,支持用户自定义、系统导入和内置模板
 */
@Data
@ToString
public class ModelTemplate {

    private Long id;

    /** 模板代码，如 "gpt-4o" */
    private String code;

    /** 显示名称 */
    private String name;

    /** 模型类型 */
    private ModelType type;

    /** 提供商代码 */
    private String providerCode;

    /** 是否支持流式 */
    private Boolean supportStream;

    /** 最大Token限制 */
    private Integer maxTokenLimit;

    /** 模型描述 */
    private String description;

    /** 能力配置（JSONB存储） */
    private Map<String, Object> capabilities;

    /** 默认参数配置（JSONB存储） */
    private Map<String, Object> defaultConfig;

    /** 标签数组 */
    private List<String> tags;

    /** 是否已弃用 */
    private Boolean deprecated;

    /** 发布日期 */
    private LocalDate releaseDate;

    /** 模板来源: USER/SYSTEM/BUILTIN */
    private TemplateSource source;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;

    // ========== 领域行为 ==========

    /**
     * 生成去重键: providerCode + ":" + code
     * 用于在导入时识别重复模板
     *
     * @return 去重键
     */
    public String getDeduplicationKey() {
        if (providerCode == null || code == null) {
            return null;
        }
        return providerCode + ":" + code;
    }

    /**
     * 从能力配置中提取supportStream
     * 兼容能力配置中的 stream/streaming 字段
     *
     * @return 是否支持流式
     */
    public Boolean extractSupportStreamFromCapabilities() {
        if (capabilities == null || capabilities.isEmpty()) {
            return false;
        }

        // 尝试从capabilities中提取stream相关字段
        Object streamValue = capabilities.get("stream");
        if (streamValue != null) {
            return Boolean.valueOf(streamValue.toString());
        }

        Object streamingValue = capabilities.get("streaming");
        if (streamingValue != null) {
            return Boolean.valueOf(streamingValue.toString());
        }

        return false;
    }

    /**
     * 是否为用户自定义模板
     *
     * @return true if 用户自定义
     */
    public boolean isUserTemplate() {
        return source == TemplateSource.USER;
    }

    /**
     * 是否为系统导入模板
     *
     * @return true if 系统导入
     */
    public boolean isSystemTemplate() {
        return source == TemplateSource.SYSTEM;
    }

    /**
     * 是否为内置模板
     *
     * @return true if 内置模板
     */
    public boolean isBuiltinTemplate() {
        return source == TemplateSource.BUILTIN;
    }

    /**
     * 是否已弃用
     *
     * @return true if 已弃用
     */
    public boolean isDeprecated() {
        return Boolean.TRUE.equals(deprecated);
    }

    /**
     * 是否支持流式
     *
     * @return true if 支持流式
     */
    public boolean supportStream() {
        return Boolean.TRUE.equals(supportStream);
    }

    /**
     * 标记为已弃用
     */
    public void markAsDeprecated() {
        this.deprecated = true;
    }

    /**
     * 取消弃用标记
     */
    public void unmarkAsDeprecated() {
        this.deprecated = false;
    }
}
