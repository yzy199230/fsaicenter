package com.fsa.aicenter.interfaces.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiImageRequest {
    private String model;

    @NotBlank(message = "prompt is required")
    private String prompt;

    @Builder.Default
    private Integer n = 1;

    private String size;
    private String quality;
    private String style;

    @JsonProperty("response_format")
    private String responseFormat;

    private String user;
}
