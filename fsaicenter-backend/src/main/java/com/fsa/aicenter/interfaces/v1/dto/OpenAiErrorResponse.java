package com.fsa.aicenter.interfaces.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiErrorResponse {
    private ErrorBody error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorBody {
        private String message;
        private String type;
        private String param;
        private String code;
    }

    public static OpenAiErrorResponse of(String message, String type, String code) {
        return OpenAiErrorResponse.builder()
                .error(ErrorBody.builder()
                        .message(message)
                        .type(type)
                        .code(code)
                        .build())
                .build();
    }
}
