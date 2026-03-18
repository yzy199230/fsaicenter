package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 热门模型响应
 */
@Data
@Schema(description = "热门模型")
public class TopModelResponse {

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "调用次数")
    private Long callCount;
}
