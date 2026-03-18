package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * API密钥统计响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "API密钥统计响应")
public class ApiKeyStatisticsResponse {

    @Schema(description = "总请求数")
    private Long totalRequests;

    @Schema(description = "成功请求数")
    private Long successRequests;

    @Schema(description = "失败请求数")
    private Long failedRequests;

    @Schema(description = "成功率(%)")
    private BigDecimal successRate;

    @Schema(description = "总Token数")
    private Long totalTokens;

    @Schema(description = "总费用(元)")
    private BigDecimal totalCost;

    @Schema(description = "平均响应时间(ms)")
    private BigDecimal avgResponseTime;
}
