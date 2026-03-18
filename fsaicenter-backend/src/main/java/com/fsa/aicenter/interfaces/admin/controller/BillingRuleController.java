package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.BillingRuleQueryRequest;
import com.fsa.aicenter.application.dto.request.CreateBillingRuleRequest;
import com.fsa.aicenter.application.dto.request.UpdateBillingRuleRequest;
import com.fsa.aicenter.application.dto.response.BillingRuleResponse;
import com.fsa.aicenter.application.service.BillingRuleManagementService;
import com.fsa.aicenter.common.annotation.OperationLog;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.PageResult;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 计费规则管理接口
 */
@Tag(name = "计费规则管理", description = "管理模型的计费规则")
@RestController
@RequestMapping("/admin/billing/rules")
@RequiredArgsConstructor
public class BillingRuleController {

    private final BillingRuleManagementService billingRuleService;

    @Operation(summary = "分页查询计费规则")
    @RequirePermission("billing:rule:list")
    @GetMapping
    public Result<PageResult<BillingRuleResponse>> listRules(BillingRuleQueryRequest request) {
        PageResult<BillingRuleResponse> result = billingRuleService.listRules(request);
        return Result.success(result);
    }

    @Operation(summary = "查询计费规则详情")
    @RequirePermission("billing:rule:view")
    @GetMapping("/{id}")
    public Result<BillingRuleResponse> getRule(@PathVariable Long id) {
        BillingRuleResponse response = billingRuleService.getRule(id);
        return Result.success(response);
    }

    @Operation(summary = "创建计费规则")
    @OperationLog(operation = "创建计费规则")
    @RequirePermission("billing:rule:create")
    @PostMapping
    public Result<Long> createRule(@Valid @RequestBody CreateBillingRuleRequest request) {
        Long ruleId = billingRuleService.createRule(request);
        return Result.success(ruleId);
    }

    @Operation(summary = "更新计费规则")
    @OperationLog(operation = "更新计费规则")
    @RequirePermission("billing:rule:update")
    @PutMapping("/{id}")
    public Result<Void> updateRule(@PathVariable Long id,
                                   @Valid @RequestBody UpdateBillingRuleRequest request) {
        billingRuleService.updateRule(id, request);
        return Result.success();
    }

    @Operation(summary = "删除计费规则")
    @OperationLog(operation = "删除计费规则")
    @RequirePermission("billing:rule:delete")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        billingRuleService.deleteRule(id);
        return Result.success();
    }
}
