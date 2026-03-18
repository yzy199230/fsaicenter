package com.fsa.aicenter.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 发现的模型响应DTO
 * <p>
 * 用于返回从Provider API自动发现的模型信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveredModelResponse {

    /**
     * 模型ID/代码（Provider返回的原始标识）
     */
    private String modelId;

    /**
     * 模型名称（如果Provider提供）
     */
    private String name;

    /**
     * 模型所有者
     */
    private String ownedBy;

    /**
     * 模型类型（推断）
     */
    private String inferredType;

    /**
     * 是否已存在于系统中
     */
    private Boolean existsInSystem;

    /**
     * 匹配到的模板（如果有）
     */
    private ModelTemplateResponse matchedTemplate;

    /**
     * 模型创建时间（来自Provider）
     */
    private LocalDateTime createdAt;

    /**
     * 额外信息
     */
    private Map<String, Object> extra;
}
