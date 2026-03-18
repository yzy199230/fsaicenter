package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 仪表板统计数据响应
 */
@Data
@Schema(description = "仪表板统计数据")
public class DashboardStatsResponse {

    @Schema(description = "今日请求数")
    private Long todayRequests;

    @Schema(description = "成功率(%)")
    private BigDecimal successRate;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "活跃API密钥数")
    private Integer activeApiKeys;
}
