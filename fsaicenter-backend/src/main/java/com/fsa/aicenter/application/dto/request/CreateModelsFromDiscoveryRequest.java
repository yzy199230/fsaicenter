package com.fsa.aicenter.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 从发现的模型创建模型请求
 */
@Data
public class CreateModelsFromDiscoveryRequest {

    /**
     * 提供商ID
     */
    @NotNull(message = "提供商ID不能为空")
    private Long providerId;

    /**
     * 要创建的模型列表（最多4个无模板模型）
     */
    @NotNull(message = "模型列表不能为空")
    @Size(min = 1, max = 50, message = "模型数量必须在1-50之间")
    @Valid
    private List<DiscoveredModelItem> models;

    @Data
    public static class DiscoveredModelItem {
        /**
         * 模型ID/代码
         */
        @NotNull(message = "模型ID不能为空")
        private String modelId;

        /**
         * 模型名称
         */
        private String name;

        /**
         * 模型类型
         */
        @NotNull(message = "模型类型不能为空")
        private String type;

        /**
         * 是否有匹配的模板
         */
        private Boolean hasTemplate;

        /**
         * 匹配的模板代码（如果有）
         */
        private String templateCode;
    }
}
