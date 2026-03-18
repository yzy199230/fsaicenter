package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.CreateModelFromTemplateRequest;
import com.fsa.aicenter.application.dto.request.SaveUserTemplateRequest;
import com.fsa.aicenter.application.dto.request.TemplateSyncRequest;
import com.fsa.aicenter.application.dto.response.ModelTemplateResponse;
import com.fsa.aicenter.application.dto.response.SyncSourceResponse;
import com.fsa.aicenter.application.dto.response.TemplateSyncResponse;
import com.fsa.aicenter.application.service.ModelTemplateService;
import com.fsa.aicenter.application.service.ModelTemplateSyncService;
import com.fsa.aicenter.infrastructure.sync.TemplateSyncResult;
import com.fsa.aicenter.common.annotation.OperationLog;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.Result;
import com.fsa.aicenter.domain.model.entity.ModelTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模型模板管理Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "模型模板管理", description = "模型模板的查询、导入和管理")
@RestController
@RequestMapping("/admin/model-templates")
@RequiredArgsConstructor
public class ModelTemplateController {

    private final ModelTemplateService templateService;
    private final ModelTemplateSyncService syncService;

    @Operation(summary = "获取模板列表")
    @RequirePermission("template:list")
    @GetMapping
    public Result<List<ModelTemplateResponse>> listTemplates(
            @Parameter(description = "提供商代码") @RequestParam(required = false) String providerCode,
            @Parameter(description = "模型类型") @RequestParam(required = false) String type,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword
    ) {
        List<ModelTemplateResponse> templates = templateService.listAllTemplates(providerCode, type, keyword);
        return Result.success(templates);
    }

    @Operation(summary = "从模板创建模型")
    @OperationLog(operation = "从模板创建模型")
    @RequirePermission("template:create-model")
    @PostMapping("/create-models")
    public Result<Integer> createModelsFromTemplates(@Valid @RequestBody CreateModelFromTemplateRequest request) {
        int count = templateService.createModelsFromTemplates(request);
        return Result.success(count);
    }

    @Operation(summary = "保存用户模板")
    @OperationLog(operation = "保存用户模板")
    @RequirePermission("template:save")
    @PostMapping
    public Result<ModelTemplateResponse> saveUserTemplate(@Valid @RequestBody SaveUserTemplateRequest request) {
        ModelTemplate template = templateService.saveUserTemplate(request);

        // 转换为响应DTO
        ModelTemplateResponse response = new ModelTemplateResponse();
        response.setId(template.getId());
        response.setCode(template.getCode());
        response.setName(template.getName());
        response.setType(template.getType() != null ? template.getType().getCode() : null);
        response.setProviderCode(template.getProviderCode());
        response.setSupportStream(template.getSupportStream());
        response.setMaxTokenLimit(template.getMaxTokenLimit());
        response.setDescription(template.getDescription());
        response.setCapabilities(template.getCapabilities());
        response.setDefaultConfig(template.getDefaultConfig());
        response.setTags(template.getTags());
        response.setDeprecated(template.getDeprecated());
        response.setReleaseDate(template.getReleaseDate());
        response.setSource(template.getSource() != null ? template.getSource().getCode() : null);

        return Result.success(response);
    }

    @Operation(summary = "删除用户模板")
    @OperationLog(operation = "删除用户模板")
    @RequirePermission("template:delete")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUserTemplate(@PathVariable Long id) {
        templateService.deleteUserTemplate(id);
        return Result.success();
    }

    @Operation(summary = "导入内置模板")
    @OperationLog(operation = "导入内置模板")
    @RequirePermission("template:import")
    @PostMapping("/import-builtin")
    public Result<Integer> importBuiltinTemplates() {
        int count = templateService.importBuiltinTemplatesToDatabase();
        return Result.success(count);
    }

    // ========== 模板同步相关接口 ==========

    @Operation(summary = "获取可用的同步数据源")
    @RequirePermission("template:sync")
    @GetMapping("/sync/sources")
    public Result<List<SyncSourceResponse>> listSyncSources() {
        List<Map<String, Object>> sources = syncService.listAvailableSources();
        List<SyncSourceResponse> responses = sources.stream()
                .map(source -> {
                    SyncSourceResponse response = new SyncSourceResponse();
                    response.setName((String) source.get("name"));
                    response.setDisplayName((String) source.get("displayName"));
                    response.setAvailable((Boolean) source.get("available"));
                    return response;
                })
                .toList();
        return Result.success(responses);
    }

    @Operation(summary = "从数据源同步模板")
    @OperationLog(operation = "同步模型模板")
    @RequirePermission("template:sync")
    @PostMapping("/sync")
    public Result<TemplateSyncResponse> syncTemplates(@Valid @RequestBody TemplateSyncRequest request) {
        TemplateSyncResult result = syncService.syncFromSource(
                request.getSource(),
                Boolean.TRUE.equals(request.getDryRun())
        );

        TemplateSyncResponse response = new TemplateSyncResponse();
        response.setSourceName(result.getSourceName());
        response.setSuccess(result.isSuccess());
        response.setAddedCount(result.getAddedCount());
        response.setUpdatedCount(result.getUpdatedCount());
        response.setUnchangedCount(result.getUnchangedCount());
        response.setSkippedCount(result.getSkippedCount());
        response.setTotalProcessed(result.getTotalProcessed());
        response.setDurationMs(result.getDurationMs());
        response.setStartTime(result.getStartTime());
        response.setEndTime(result.getEndTime());
        response.setErrors(result.getErrors());

        return Result.success(response);
    }

    @Operation(summary = "检查数据源是否可用")
    @RequirePermission("template:sync")
    @GetMapping("/sync/check/{sourceName}")
    public Result<Boolean> checkSourceAvailable(@PathVariable String sourceName) {
        boolean available = syncService.isSourceAvailable(sourceName);
        return Result.success(available);
    }
}
