package com.fsa.aicenter.interfaces.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fsa.aicenter.infrastructure.adapter.common.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiChatRequest {
    @NotBlank(message = "model is required")
    private String model;

    @NotEmpty(message = "messages is required")
    private List<Message> messages;

    private Boolean stream;

    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    private List<String> stop;

    private String user;

    public boolean isStream() {
        return stream != null && stream;
    }
}
