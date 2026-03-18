package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.response.*;
import com.fsa.aicenter.application.service.DashboardService;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仪表板Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "仪表板", description = "监控统计数据接口")
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取统计数据")
    @RequirePermission("dashboard:view")
    @GetMapping("/stats")
    public Result<DashboardStatsResponse> getStats() {
        DashboardStatsResponse stats = dashboardService.getStats();
        return Result.success(stats);
    }

    @Operation(summary = "获取请求趋势")
    @RequirePermission("dashboard:view")
    @GetMapping("/request-trend")
    public Result<DashboardTrendResponse> getRequestTrend(
            @Parameter(description = "天数，默认7天") @RequestParam(defaultValue = "7") Integer days) {
        DashboardTrendResponse trend = dashboardService.getRequestTrend(days);
        return Result.success(trend);
    }

    @Operation(summary = "获取模型使用分布")
    @RequirePermission("dashboard:view")
    @GetMapping("/model-distribution")
    public Result<List<ModelDistributionResponse>> getModelDistribution() {
        List<ModelDistributionResponse> distribution = dashboardService.getModelDistribution();
        return Result.success(distribution);
    }

    @Operation(summary = "获取热门模型")
    @RequirePermission("dashboard:view")
    @GetMapping("/top-models")
    public Result<List<TopModelResponse>> getTopModels(
            @Parameter(description = "数量限制，默认10") @RequestParam(defaultValue = "10") Integer limit) {
        List<TopModelResponse> topModels = dashboardService.getTopModels(limit);
        return Result.success(topModels);
    }

    @Operation(summary = "获取最近日志")
    @RequirePermission("dashboard:view")
    @GetMapping("/recent-logs")
    public Result<List<RecentLogResponse>> getRecentLogs(
            @Parameter(description = "数量限制，默认10") @RequestParam(defaultValue = "10") Integer limit) {
        List<RecentLogResponse> recentLogs = dashboardService.getRecentLogs(limit);
        return Result.success(recentLogs);
    }
}
