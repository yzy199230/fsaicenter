package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.response.AdminPermissionResponse;
import com.fsa.aicenter.application.service.AdminPermissionManagementService;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "权限管理", description = "管理系统权限的查询")
@RestController
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final AdminPermissionManagementService permissionManagementService;

    @Operation(summary = "获取权限树")
    @RequirePermission("permission:list")
    @GetMapping("/tree")
    public Result<List<AdminPermissionResponse>> getPermissionTree() {
        List<AdminPermissionResponse> tree = permissionManagementService.getPermissionTree();
        return Result.success(tree);
    }

    @Operation(summary = "获取所有权限（平铺）")
    @RequirePermission("permission:list")
    @GetMapping
    public Result<List<AdminPermissionResponse>> listAllPermissions() {
        List<AdminPermissionResponse> permissions = permissionManagementService.listAllPermissions();
        return Result.success(permissions);
    }

    @Operation(summary = "获取角色权限")
    @RequirePermission("permission:list")
    @GetMapping("/role/{roleId}")
    public Result<List<AdminPermissionResponse>> getPermissionsByRoleId(@PathVariable Long roleId) {
        List<AdminPermissionResponse> permissions = permissionManagementService.getPermissionsByRoleId(roleId);
        return Result.success(permissions);
    }
}
