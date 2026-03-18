package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 计费趋势数据响应
 */
@Data
@Schema(description = "计费趋势数据")
public class BillingTrendResponse {

    @Schema(description = "日期")
    private String date;

    @Schema(description = "成本")
    private BigDecimal cost;

    @Schema(description = "请求数")
    private Long requests;
}
