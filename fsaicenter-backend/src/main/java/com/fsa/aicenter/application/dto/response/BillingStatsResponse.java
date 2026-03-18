package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计费统计数据响应
 */
@Data
@Schema(description = "计费统计数据")
public class BillingStatsResponse {

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "总请求数")
    private Long totalRequests;

    @Schema(description = "总使用量（Token数）")
    private Long totalUsage;

    @Schema(description = "平均单价")
    private BigDecimal avgUnitPrice;
}
