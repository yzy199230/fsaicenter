package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.CreateModelApiKeyRequest;
import com.fsa.aicenter.application.dto.request.UpdateModelApiKeyRequest;
import com.fsa.aicenter.application.dto.response.ModelApiKeyResponse;
import com.fsa.aicenter.application.service.ModelApiKeyManagementService;
import com.fsa.aicenter.common.annotation.OperationLog;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型API Key管理控制器
 *
 * @author FSA AI Center
 */
@Tag(name = "模型Key管理", description = "管理模型的API Key池")
@RestController
@RequestMapping("/admin/models/{modelId}/keys")
@RequiredArgsConstructor
public class ModelApiKeyController {

    private final ModelApiKeyManagementService modelApiKeyManagementService;

    @Operation(summary = "添加API Key")
    @OperationLog(operation = "添加模型API Key")
    @PostMapping
    public Result<ModelApiKeyResponse> create(
            @PathVariable Long modelId,
            @Valid @RequestBody CreateModelApiKeyRequest request
    ) {
        request.setModelId(modelId);
        ModelApiKeyResponse response = modelApiKeyManagementService.create(request);
        return Result.success(response);
    }

    @Operation(summary = "更新API Key")
    @OperationLog(operation = "更新模型API Key")
    @PutMapping("/{keyId}")
    public Result<Void> update(
            @PathVariable Long modelId,
            @PathVariable Long keyId,
            @Valid @RequestBody UpdateModelApiKeyRequest request
    ) {
        modelApiKeyManagementService.update(keyId, request);
        return Result.success();
    }

    @Operation(summary = "删除API Key")
    @OperationLog(operation = "删除模型API Key")
    @DeleteMapping("/{keyId}")
    public Result<Void> delete(
            @PathVariable Long modelId,
            @PathVariable Long keyId
    ) {
        modelApiKeyManagementService.delete(keyId);
        return Result.success();
    }

    @Operation(summary = "启用/禁用API Key")
    @OperationLog(operation = "切换模型API Key状态")
    @PutMapping("/{keyId}/status")
    public Result<Void> toggleStatus(
            @PathVariable Long modelId,
            @PathVariable Long keyId
    ) {
        modelApiKeyManagementService.toggleStatus(keyId);
        return Result.success();
    }

    @Operation(summary = "重置健康状态")
    @OperationLog(operation = "重置模型API Key健康状态")
    @PutMapping("/{keyId}/health/reset")
    public Result<Void> resetHealthStatus(
            @PathVariable Long modelId,
            @PathVariable Long keyId
    ) {
        modelApiKeyManagementService.resetHealthStatus(keyId);
        return Result.success();
    }

    @Operation(summary = "获取模型的所有Key")
    @GetMapping
    public Result<List<ModelApiKeyResponse>> list(@PathVariable Long modelId) {
        List<ModelApiKeyResponse> keys = modelApiKeyManagementService.listByModelId(modelId);
        return Result.success(keys);
    }

    @Operation(summary = "获取Key详情")
    @GetMapping("/{keyId}")
    public Result<ModelApiKeyResponse> getById(
            @PathVariable Long modelId,
            @PathVariable Long keyId
    ) {
        ModelApiKeyResponse response = modelApiKeyManagementService.getById(keyId);
        return Result.success(response);
    }
}
