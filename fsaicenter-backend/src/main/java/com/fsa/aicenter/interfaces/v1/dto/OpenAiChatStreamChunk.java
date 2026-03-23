package com.fsa.aicenter.interfaces.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiChatStreamChunk {
    private String id;

    @Builder.Default
    private String object = "chat.completion.chunk";

    private Long created;
    private String model;
    private List<StreamChoice> choices;
    private OpenAiUsage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StreamChoice {
        private int index;
        private Delta delta;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Delta {
        private String role;
        private String content;
    }
}
