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
public class OpenAiModelListResponse {
    @Builder.Default
    private String object = "list";

    private List<OpenAiModelObject> data;
}
