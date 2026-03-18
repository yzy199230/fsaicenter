package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.CreateApiKeyRequest;
import com.fsa.aicenter.application.dto.request.UpdateApiKeyRequest;
import com.fsa.aicenter.application.dto.request.UpdateStatusRequest;
import com.fsa.aicenter.application.dto.response.*;
import com.fsa.aicenter.application.service.ApiKeyManagementService;
import com.fsa.aicenter.application.service.ApiKeyStatisticsService;
import com.fsa.aicenter.common.annotation.OperationLog;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.PageResult;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * API密钥管理Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "API密钥管理", description = "管理API密钥的增删改查")
@RestController
@RequestMapping("/admin/apikeys")
@RequiredArgsConstructor
public class ApiKeyManagementController {

    private final ApiKeyManagementService apiKeyManagementService;
    private final ApiKeyStatisticsService apiKeyStatisticsService;

    @Operation(summary = "创建API密钥")
    @OperationLog(operation = "创建API密钥")
    @RequirePermission("apikey:create")
    @PostMapping
    public Result<Long> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        Long apiKeyId = apiKeyManagementService.createApiKey(request);
        return Result.success(apiKeyId);
    }

    @Operation(summary = "更新API密钥")
    @OperationLog(operation = "更新API密钥")
    @RequirePermission("apikey:update")
    @PutMapping("/{id}")
    public Result<Void> updateApiKey(@PathVariable Long id,
                                     @Valid @RequestBody UpdateApiKeyRequest request) {
        apiKeyManagementService.updateApiKey(id, request);
        return Result.success();
    }

    @Operation(summary = "删除API密钥")
    @OperationLog(operation = "删除API密钥")
    @RequirePermission("apikey:delete")
    @DeleteMapping("/{id}")
    public Result<Void> deleteApiKey(@PathVariable Long id) {
        apiKeyManagementService.deleteApiKey(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用API密钥")
    @OperationLog(operation = "切换API密钥状态")
    @RequirePermission("apikey:status")
    @PutMapping("/{id}/status")
    public Result<Void> toggleApiKeyStatus(@PathVariable Long id,
                                            @RequestBody UpdateStatusRequest request) {
        apiKeyManagementService.updateApiKeyStatus(id, request.getStatus());
        return Result.success();
    }

    @Operation(summary = "重置配额")
    @OperationLog(operation = "重置API密钥配额")
    @RequirePermission("apikey:reset")
    @PutMapping("/{id}/quota/reset")
    public Result<Void> resetQuota(@PathVariable Long id) {
        apiKeyManagementService.resetQuota(id);
        return Result.success();
    }

    @Operation(summary = "查询API密钥详情")
    @RequirePermission("apikey:view")
    @GetMapping("/{id}")
    public Result<ApiKeyResponse> getApiKey(@PathVariable Long id) {
        ApiKeyResponse response = apiKeyManagementService.getApiKey(id);
        return Result.success(response);
    }

    @Operation(summary = "查询API密钥列表")
    @RequirePermission("apikey:list")
    @GetMapping
    public Result<List<ApiKeyResponse>> listApiKeys() {
        List<ApiKeyResponse> apiKeys = apiKeyManagementService.listApiKeys();
        return Result.success(apiKeys);
    }

    @Operation(summary = "获取API密钥使用统计")
    @RequirePermission("apikey:view")
    @GetMapping("/{id}/statistics")
    public Result<ApiKeyStatisticsResponse> getStatistics(
            @PathVariable Long id,
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
        ApiKeyStatisticsResponse stats = apiKeyStatisticsService.getStatistics(id, startTime, endTime);
        return Result.success(stats);
    }

    @Operation(summary = "获取API密钥使用趋势")
    @RequirePermission("apikey:view")
    @GetMapping("/{id}/usage-trend")
    public Result<List<ApiKeyUsageTrendResponse>> getUsageTrend(
            @PathVariable Long id,
            @Parameter(description = "天数，默认7天")
            @RequestParam(defaultValue = "7") Integer days) {
        List<ApiKeyUsageTrendResponse> trend = apiKeyStatisticsService.getUsageTrend(id, days);
        return Result.success(trend);
    }

    @Operation(summary = "获取API密钥模型分布")
    @RequirePermission("apikey:view")
    @GetMapping("/{id}/model-distribution")
    public Result<List<ApiKeyModelDistributionResponse>> getModelDistribution(
            @PathVariable Long id,
            @Parameter(description = "开始时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime startTime,
            @Parameter(description = "结束时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime endTime) {
        // 默认查询最近30天
        if (startTime == null) {
            startTime = LocalDate.now().minusDays(30).atStartOfDay();
        }
        if (endTime == null) {
            endTime = LocalDate.now().atTime(LocalTime.MAX);
        }
        List<ApiKeyModelDistributionResponse> distribution = apiKeyStatisticsService.getModelDistribution(id, startTime, endTime);
        return Result.success(distribution);
    }

    @Operation(summary = "获取API密钥请求日志")
    @RequirePermission("apikey:view")
    @GetMapping("/{id}/logs")
    public Result<PageResult<ApiKeyLogResponse>> getLogs(
            @PathVariable Long id,
            @Parameter(description = "页码")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "状态筛选(SUCCESS/FAILED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "开始时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime startTime,
            @Parameter(description = "结束时间")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime endTime) {
        // 默认查询最近7天
        if (startTime == null) {
            startTime = LocalDate.now().minusDays(7).atStartOfDay();
        }
        if (endTime == null) {
            endTime = LocalDate.now().atTime(LocalTime.MAX);
        }
        PageResult<ApiKeyLogResponse> logs = apiKeyStatisticsService.getLogs(id, page, size, status, startTime, endTime);
        return Result.success(logs);
    }
}
