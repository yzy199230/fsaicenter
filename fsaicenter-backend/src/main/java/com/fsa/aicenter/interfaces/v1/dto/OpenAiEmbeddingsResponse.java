package com.fsa.aicenter.interfaces.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiEmbeddingsResponse {
    @Builder.Default
    private String object = "list";

    private List<EmbeddingData> data;
    private String model;
    private OpenAiUsage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingData {
        @Builder.Default
        private String object = "embedding";

        private List<Double> embedding;
        private int index;
    }
}
