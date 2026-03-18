package com.fsa.aicenter.domain.model.valueobject;

import lombok.Value;

/**
 * 模型配置值对象(不可变)
 */
@Value
public class ModelConfig {
    Double temperature;
    Integer maxTokens;
    Double topP;
    String systemPrompt;

    /**
     * 合并配置(请求配置优先)
     */
    public ModelConfig merge(ModelConfig requestConfig) {
        if (requestConfig == null) {
            return this;
        }
        return new ModelConfig(
            requestConfig.temperature != null ? requestConfig.temperature : this.temperature,
            requestConfig.maxTokens != null ? requestConfig.maxTokens : this.maxTokens,
            requestConfig.topP != null ? requestConfig.topP : this.topP,
            requestConfig.systemPrompt != null ? requestConfig.systemPrompt : this.systemPrompt
        );
    }
}
