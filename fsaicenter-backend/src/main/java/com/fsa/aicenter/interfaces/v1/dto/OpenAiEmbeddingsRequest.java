package com.fsa.aicenter.interfaces.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiEmbeddingsRequest {
    @NotBlank(message = "model is required")
    private String model;

    @NotNull(message = "input is required")
    private Object input; // String or List<String>

    @JsonProperty("encoding_format")
    private String encodingFormat;

    private Integer dimensions;
}
