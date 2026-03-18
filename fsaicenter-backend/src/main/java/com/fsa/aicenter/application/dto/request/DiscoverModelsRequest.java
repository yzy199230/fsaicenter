package com.fsa.aicenter.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发现模型请求DTO
 */
@Data
public class DiscoverModelsRequest {

    /**
     * 提供商ID
     */
    @NotNull(message = "提供商ID不能为空")
    private Long providerId;

    /**
     * API Key（可选，用于需要认证的提供商）
     */
    private String apiKey;
}
