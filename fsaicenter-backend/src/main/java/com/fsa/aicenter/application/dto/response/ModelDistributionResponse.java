package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模型使用分布响应
 */
@Data
@Schema(description = "模型使用分布")
public class ModelDistributionResponse {

    @Schema(description = "模型名称")
    private String name;

    @Schema(description = "调用次数")
    private Long value;
}
