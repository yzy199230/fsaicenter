package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.response.BillingStatsResponse;
import com.fsa.aicenter.application.dto.response.BillingTrendResponse;
import com.fsa.aicenter.application.dto.response.ModelCostResponse;
import com.fsa.aicenter.application.service.BillingStatisticsService;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 计费统计Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "计费统计", description = "计费统计数据接口")
@RestController
@RequestMapping("/admin/billing/statistics")
@RequiredArgsConstructor
public class BillingStatisticsController {

    private final BillingStatisticsService billingStatisticsService;

    @Operation(summary = "获取计费统计")
    @RequirePermission("billing:view")
    @GetMapping("/stats")
    public Result<BillingStatsResponse> getStats(
            @Parameter(description = "开始时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime startTime,
            @Parameter(description = "结束时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime endTime) {
        // 默认查询今日
        if (startTime == null) {
            startTime = LocalDate.now().atStartOfDay();
        }
        if (endTime == null) {
            endTime = LocalDate.now().atTime(LocalTime.MAX);
        }
        BillingStatsResponse stats = billingStatisticsService.getBillingStats(startTime, endTime);
        return Result.success(stats);
    }

    @Operation(summary = "获取计费趋势")
    @RequirePermission("billing:view")
    @GetMapping("/trend")
    public Result<List<BillingTrendResponse>> getTrend(
            @Parameter(description = "天数，默认7天")
            @RequestParam(defaultValue = "7") Integer days) {
        List<BillingTrendResponse> trend = billingStatisticsService.getBillingTrend(days);
        return Result.success(trend);
    }

    @Operation(summary = "获取模型成本统计")
    @RequirePermission("billing:view")
    @GetMapping("/model-cost")
    public Result<List<ModelCostResponse>> getModelCost(
            @Parameter(description = "开始时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime startTime,
            @Parameter(description = "结束时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime endTime) {
        // 默认查询今日
        if (startTime == null) {
            startTime = LocalDate.now().atStartOfDay();
        }
        if (endTime == null) {
            endTime = LocalDate.now().atTime(LocalTime.MAX);
        }
        List<ModelCostResponse> modelCost = billingStatisticsService.getModelCostStats(startTime, endTime);
        return Result.success(modelCost);
    }

    @Operation(summary = "获取API密钥成本统计")
    @RequirePermission("billing:view")
    @GetMapping("/apikey-cost/{apiKeyId}")
    public Result<BillingStatsResponse> getApiKeyCost(
            @Parameter(description = "API密钥ID")
            @PathVariable Long apiKeyId,
            @Parameter(description = "开始时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime startTime,
            @Parameter(description = "结束时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime endTime) {
        // 默认查询今日
        if (startTime == null) {
            startTime = LocalDate.now().atStartOfDay();
        }
        if (endTime == null) {
            endTime = LocalDate.now().atTime(LocalTime.MAX);
        }
        BillingStatsResponse stats = billingStatisticsService.getApiKeyCostStats(apiKeyId, startTime, endTime);
        return Result.success(stats);
    }
}
