package com.fsa.aicenter.interfaces.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiModelObject {
    private String id;

    @Builder.Default
    private String object = "model";

    private Long created;

    @JsonProperty("owned_by")
    private String ownedBy;
}
