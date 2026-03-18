package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.CreateModelRequest;
import com.fsa.aicenter.application.dto.request.ModelQueryRequest;
import com.fsa.aicenter.application.dto.request.TestModelRequest;
import com.fsa.aicenter.application.dto.request.UpdateModelRequest;
import com.fsa.aicenter.application.dto.response.ModelResponse;
import com.fsa.aicenter.application.dto.response.TestModelResponse;
import com.fsa.aicenter.application.service.ModelManagementService;
import com.fsa.aicenter.common.annotation.OperationLog;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 模型管理Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "模型管理", description = "管理AI模型的增删改查")
@RestController
@RequestMapping("/admin/models")
@RequiredArgsConstructor
public class ModelManagementController {

    private final ModelManagementService modelManagementService;

    @Operation(summary = "创建模型")
    @OperationLog(operation = "创建模型")
    @RequirePermission("model:create")
    @PostMapping
    public Result<Long> createModel(@Valid @RequestBody CreateModelRequest request) {
        Long modelId = modelManagementService.createModel(request);
        return Result.success(modelId);
    }

    @Operation(summary = "更新模型")
    @OperationLog(operation = "更新模型")
    @RequirePermission("model:update")
    @PutMapping("/{id}")
    public Result<Void> updateModel(@PathVariable Long id,
                                    @Valid @RequestBody UpdateModelRequest request) {
        modelManagementService.updateModel(id, request);
        return Result.success();
    }

    @Operation(summary = "删除模型")
    @OperationLog(operation = "删除模型")
    @RequirePermission("model:delete")
    @DeleteMapping("/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        modelManagementService.deleteModel(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用模型")
    @OperationLog(operation = "切换模型状态")
    @RequirePermission("model:status")
    @PutMapping("/{id}/status")
    public Result<Void> toggleModelStatus(@PathVariable Long id) {
        modelManagementService.toggleModelStatus(id);
        return Result.success();
    }

    @Operation(summary = "查询模型详情")
    @RequirePermission("model:view")
    @GetMapping("/{id}")
    public Result<ModelResponse> getModel(@PathVariable Long id) {
        ModelResponse response = modelManagementService.getModel(id);
        return Result.success(response);
    }

    @Operation(summary = "查询模型列表")
    @RequirePermission("model:list")
    @GetMapping
    public Result<List<ModelResponse>> listModels(ModelQueryRequest request) {
        List<ModelResponse> models = modelManagementService.listModels(request);
        return Result.success(models);
    }

    @Operation(summary = "测试模型")
    @OperationLog(operation = "测试模型")
    @RequirePermission("model:test")
    @PostMapping("/{id}/test")
    public Result<TestModelResponse> testModel(@PathVariable Long id,
                                               @Valid @RequestBody TestModelRequest request) {
        TestModelResponse response = modelManagementService.testModel(id, request);
        return Result.success(response);
    }

    @Operation(summary = "流式测试模型")
    @RequirePermission("model:test")
    @PostMapping(value = "/{id}/test/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter testModelStream(@PathVariable Long id,
                                      @Valid @RequestBody TestModelRequest request) {
        return modelManagementService.testModelStream(id, request);
    }

    @Operation(summary = "更新模型排序")
    @OperationLog(operation = "更新模型排序")
    @RequirePermission("model:update")
    @PutMapping("/{id}/sort-order")
    public Result<Void> updateSortOrder(@PathVariable Long id,
                                        @RequestBody java.util.Map<String, Integer> request) {
        Integer sortOrder = request.get("sortOrder");
        if (sortOrder == null) {
            return Result.error("sortOrder不能为空");
        }
        modelManagementService.updateSortOrder(id, sortOrder);
        return Result.success();
    }
}
