package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.CreateProviderRequest;
import com.fsa.aicenter.application.dto.request.UpdateProviderRequest;
import com.fsa.aicenter.application.dto.response.ProviderResponse;
import com.fsa.aicenter.application.service.ProviderManagementService;
import com.fsa.aicenter.common.annotation.OperationLog;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提供商管理Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "提供商管理", description = "管理AI提供商的增删改查")
@RestController
@RequestMapping("/admin/providers")
@RequiredArgsConstructor
public class ProviderManagementController {

    private final ProviderManagementService providerManagementService;

    @Operation(summary = "创建提供商")
    @OperationLog(operation = "创建提供商")
    @RequirePermission("provider:create")
    @PostMapping
    public Result<Long> createProvider(@Valid @RequestBody CreateProviderRequest request) {
        Long providerId = providerManagementService.createProvider(request);
        return Result.success(providerId);
    }

    @Operation(summary = "更新提供商")
    @OperationLog(operation = "更新提供商")
    @RequirePermission("provider:update")
    @PutMapping("/{id}")
    public Result<Void> updateProvider(@PathVariable Long id,
                                       @Valid @RequestBody UpdateProviderRequest request) {
        providerManagementService.updateProvider(id, request);
        return Result.success();
    }

    @Operation(summary = "删除提供商")
    @OperationLog(operation = "删除提供商")
    @RequirePermission("provider:delete")
    @DeleteMapping("/{id}")
    public Result<Void> deleteProvider(@PathVariable Long id) {
        providerManagementService.deleteProvider(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用提供商")
    @OperationLog(operation = "切换提供商状态")
    @RequirePermission("provider:status")
    @PutMapping("/{id}/status")
    public Result<Void> toggleProviderStatus(@PathVariable Long id) {
        providerManagementService.toggleProviderStatus(id);
        return Result.success();
    }

    @Operation(summary = "获取所有提供商选项（下拉框用）")
    @RequirePermission("provider:list")
    @GetMapping("/all")
    public Result<List<ProviderResponse>> getAllProviders() {
        List<ProviderResponse> providers = providerManagementService.listProviders();
        return Result.success(providers);
    }

    @Operation(summary = "查询提供商详情")
    @RequirePermission("provider:view")
    @GetMapping("/{id}")
    public Result<ProviderResponse> getProvider(@PathVariable Long id) {
        ProviderResponse response = providerManagementService.getProvider(id);
        return Result.success(response);
    }

    @Operation(summary = "查询提供商列表")
    @RequirePermission("provider:list")
    @GetMapping
    public Result<List<ProviderResponse>> listProviders() {
        List<ProviderResponse> providers = providerManagementService.listProviders();
        return Result.success(providers);
    }
}
