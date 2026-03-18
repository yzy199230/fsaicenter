package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.CreateModelsFromDiscoveryRequest;
import com.fsa.aicenter.application.dto.request.DiscoverModelsRequest;
import com.fsa.aicenter.application.dto.response.DiscoveredModelResponse;
import com.fsa.aicenter.application.service.ModelDiscoveryService;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型发现管理接口
 * <p>
 * 提供从AI提供商API自动发现模型的功能
 */
@Tag(name = "模型发现管理", description = "自动发现AI提供商可用模型")
@Slf4j
@RestController
@RequestMapping("/admin/model-discovery")
@RequiredArgsConstructor
public class ModelDiscoveryController {

    private final ModelDiscoveryService modelDiscoveryService;

    /**
     * 发现提供商的可用模型
     */
    @Operation(summary = "发现模型", description = "调用提供商API自动发现可用的模型列表")
    @PostMapping("/discover")
    public Result<List<DiscoveredModelResponse>> discoverModels(
            @Valid @RequestBody DiscoverModelsRequest request) {
        log.info("发现模型请求: providerId={}", request.getProviderId());
        List<DiscoveredModelResponse> models = modelDiscoveryService.discoverModels(
                request.getProviderId(), request.getApiKey());
        return Result.success(models);
    }

    /**
     * 检查提供商是否支持模型发现
     */
    @Operation(summary = "检查发现支持", description = "检查指定提供商是否支持自动发现模型")
    @GetMapping("/supports/{providerId}")
    public Result<Boolean> supportsDiscovery(
            @Parameter(description = "提供商ID") @PathVariable Long providerId) {
        boolean supports = modelDiscoveryService.supportsDiscovery(providerId);
        return Result.success(supports);
    }

    /**
     * 从发现的模型创建模型记录
     */
    @Operation(summary = "导入发现的模型", description = "将发现的模型导入系统，支持有模板和无模板两种方式")
    @PostMapping("/create")
    public Result<Integer> createModelsFromDiscovery(
            @Valid @RequestBody CreateModelsFromDiscoveryRequest request) {
        log.info("导入发现的模型请求: providerId={}, modelCount={}",
                request.getProviderId(), request.getModels().size());
        int count = modelDiscoveryService.createModelsFromDiscovery(request);
        return Result.success(count);
    }
}
