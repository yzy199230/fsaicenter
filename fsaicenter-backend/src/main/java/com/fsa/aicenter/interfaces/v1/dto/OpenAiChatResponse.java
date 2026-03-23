package com.fsa.aicenter.interfaces.v1.dto;

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
public class OpenAiChatResponse {
    private String id;

    @Builder.Default
    private String object = "chat.completion";

    private Long created;
    private String model;
    private List<Choice> choices;
    private OpenAiUsage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private int index;
        private ChatMessage message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
