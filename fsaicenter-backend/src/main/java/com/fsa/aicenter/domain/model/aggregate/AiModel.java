package com.fsa.aicenter.domain.model.aggregate;

import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelConfig;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI模型聚合根
 */
@Data
public class AiModel {
    private Long id;
    private String code;
    private String name;
    private ModelType type;
    private Long providerId;
    private ModelConfig config;
    private Boolean supportStream;
    private Integer maxTokenLimit;  // 模型支持的最大Token数限制
    private String description;
    private Integer sortOrder;
    private EntityStatus status;
    /** 模型能力配置（JSONB） */
    private Map<String, Object> capabilities;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // ========== 领域行为 ==========

    /**
     * 是否可处理指定类型的请求
     */
    public boolean canHandleRequest(ModelType requestType) {
        return this.type == requestType && isEnabled();
    }

    /**
     * 是否支持流式
     */
    public boolean supportStream() {
        return Boolean.TRUE.equals(this.supportStream);
    }

    /**
     * 合并请求配置
     */
    public ModelConfig mergeConfig(ModelConfig requestConfig) {
        if (this.config == null) {
            return requestConfig;
        }
        return this.config.merge(requestConfig);
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return status != null && status.isEnabled();
    }

    /**
     * 启用模型
     */
    public void enable() {
        this.status = EntityStatus.ENABLED;
    }

    /**
     * 禁用模型
     */
    public void disable() {
        this.status = EntityStatus.DISABLED;
    }
}
