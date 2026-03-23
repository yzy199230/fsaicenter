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
public class OpenAiImageResponse {
    private Long created;
    private List<ImageData> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageData {
        private String url;

        @JsonProperty("b64_json")
        private String b64Json;

        @JsonProperty("revised_prompt")
        private String revisedPrompt;
    }
}
