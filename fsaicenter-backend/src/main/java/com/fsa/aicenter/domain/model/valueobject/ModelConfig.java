package com.fsa.aicenter.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/**
 * 模型配置值对象(不可变)
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelConfig {
    Double temperature;
    Integer maxTokens;
    Double topP;
    String systemPrompt;

    @JsonCreator
    public ModelConfig(
            @JsonProperty("temperature") Double temperature,
            @JsonProperty("max_tokens") Integer maxTokens,
            @JsonProperty("top_p") Double topP,
            @JsonProperty("system_prompt") String systemPrompt) {
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.topP = topP;
        this.systemPrompt = systemPrompt;
    }

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
