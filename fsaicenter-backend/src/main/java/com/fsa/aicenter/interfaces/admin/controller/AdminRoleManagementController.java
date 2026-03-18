package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.CreateAdminRoleRequest;
import com.fsa.aicenter.application.dto.request.UpdateAdminRoleRequest;
import com.fsa.aicenter.application.dto.response.AdminRoleResponse;
import com.fsa.aicenter.application.service.AdminRoleManagementService;
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
 * 角色管理Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "角色管理", description = "管理系统角色的增删改查")
@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class AdminRoleManagementController {

    private final AdminRoleManagementService roleManagementService;

    @Operation(summary = "获取所有角色（下拉框用）")
    @RequirePermission("role:list")
    @GetMapping("/all")
    public Result<List<AdminRoleResponse>> getAllRoles() {
        List<AdminRoleResponse> roles = roleManagementService.listAllRoles();
        return Result.success(roles);
    }

    @Operation(summary = "查询角色列表")
    @RequirePermission("role:list")
    @GetMapping
    public Result<List<AdminRoleResponse>> listRoles(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "100") int pageSize) {
        return Result.success(roleManagementService.listRoles(keyword, status, pageNum, pageSize).getList());
    }

    @Operation(summary = "获取角色详情")
    @RequirePermission("role:view")
    @GetMapping("/{id}")
    public Result<AdminRoleResponse> getRole(@PathVariable Long id) {
        AdminRoleResponse response = roleManagementService.getRole(id);
        return Result.success(response);
    }

    @Operation(summary = "创建角色")
    @OperationLog(operation = "创建角色")
    @RequirePermission("role:create")
    @PostMapping
    public Result<Long> createRole(@Valid @RequestBody CreateAdminRoleRequest request) {
        Long roleId = roleManagementService.createRole(request);
        return Result.success(roleId);
    }

    @Operation(summary = "更新角色")
    @OperationLog(operation = "更新角色")
    @RequirePermission("role:update")
    @PutMapping("/{id}")
    public Result<Void> updateRole(@PathVariable Long id,
                                   @Valid @RequestBody UpdateAdminRoleRequest request) {
        roleManagementService.updateRole(id, request);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @OperationLog(operation = "删除角色")
    @RequirePermission("role:delete")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleManagementService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "切换角色状态")
    @OperationLog(operation = "切换角色状态")
    @RequirePermission("role:status")
    @PutMapping("/{id}/status")
    public Result<Void> toggleRoleStatus(@PathVariable Long id) {
        roleManagementService.toggleRoleStatus(id);
        return Result.success();
    }

    @Operation(summary = "分配角色权限")
    @OperationLog(operation = "分配角色权限")
    @RequirePermission("role:assign-permission")
    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                          @RequestBody List<Long> permissionIds) {
        roleManagementService.assignPermissions(id, permissionIds);
        return Result.success();
    }
}
