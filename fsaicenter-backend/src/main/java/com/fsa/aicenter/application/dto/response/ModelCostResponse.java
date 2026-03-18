package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型成本统计响应
 */
@Data
@Schema(description = "模型成本统计")
public class ModelCostResponse {

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "请求数")
    private Long requests;

    @Schema(description = "使用量")
    private Long usage;
}
