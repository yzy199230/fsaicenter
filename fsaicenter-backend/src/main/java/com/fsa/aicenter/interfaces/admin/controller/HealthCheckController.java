package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.service.ModelHealthCheckService;
import com.fsa.aicenter.application.service.ModelHealthCheckService.HealthCheckResult;
import com.fsa.aicenter.common.annotation.OperationLog;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * 健康检查管理控制器
 * <p>
 * 提供手动触发健康检查的接口
 * </p>
 *
 * @author FSA AI Center
 */
@Tag(name = "健康检查", description = "模型API Key健康检查管理")
@RestController
@RequestMapping("/admin/health-check")
@RequiredArgsConstructor
public class HealthCheckController {

    private final ModelHealthCheckService modelHealthCheckService;

    @Operation(summary = "执行全量健康检查", description = "检查所有启用模型的API Key健康状态")
    @OperationLog(operation = "执行全量健康检查")
    @PostMapping("/all")
    public Result<HealthCheckResult> checkAll() {
        HealthCheckResult result = modelHealthCheckService.performHealthCheck();
        return Result.success(result);
    }

    @Operation(summary = "检查单个模型", description = "检查指定模型的所有API Key健康状态")
    @OperationLog(operation = "检查单个模型健康状态")
    @PostMapping("/models/{modelId}")
    public Result<String> checkModel(@PathVariable Long modelId) {
        CompletableFuture<HealthCheckResult> future = modelHealthCheckService.checkModel(modelId);
        // 异步执行，立即返回
        return Result.success("健康检查任务已提交，请稍后查看结果");
    }

    @Operation(summary = "检查单个Key", description = "检查指定API Key的健康状态")
    @OperationLog(operation = "检查单个Key健康状态")
    @PostMapping("/keys/{keyId}")
    public Result<Boolean> checkKey(@PathVariable Long keyId) {
        boolean healthy = modelHealthCheckService.checkKey(keyId);
        return Result.success(healthy);
    }
}
