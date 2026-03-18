package com.fsa.aicenter.interfaces.admin.controller;

import com.fsa.aicenter.application.dto.request.AdminUserQueryRequest;
import com.fsa.aicenter.application.dto.request.CreateAdminUserRequest;
import com.fsa.aicenter.application.dto.request.UpdateAdminUserRequest;
import com.fsa.aicenter.application.dto.response.AdminUserResponse;
import com.fsa.aicenter.application.service.AdminUserManagementService;
import com.fsa.aicenter.common.annotation.OperationLog;
import com.fsa.aicenter.common.annotation.RequirePermission;
import com.fsa.aicenter.common.model.PageResult;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理Controller
 *
 * @author FSA AI Center
 */
@Tag(name = "用户管理", description = "管理系统用户的增删改查")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserManagementController {

    private final AdminUserManagementService userManagementService;

    @Operation(summary = "查询用户列表")
    @RequirePermission("user:list")
    @GetMapping
    public Result<PageResult<AdminUserResponse>> listUsers(AdminUserQueryRequest request) {
        PageResult<AdminUserResponse> result = userManagementService.listUsers(request);
        return Result.success(result);
    }

    @Operation(summary = "获取用户详情")
    @RequirePermission("user:view")
    @GetMapping("/{id}")
    public Result<AdminUserResponse> getUser(@PathVariable Long id) {
        AdminUserResponse response = userManagementService.getUser(id);
        return Result.success(response);
    }

    @Operation(summary = "创建用户")
    @OperationLog(operation = "创建用户")
    @RequirePermission("user:create")
    @PostMapping
    public Result<Long> createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        Long userId = userManagementService.createUser(request);
        return Result.success(userId);
    }

    @Operation(summary = "更新用户")
    @OperationLog(operation = "更新用户")
    @RequirePermission("user:update")
    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id,
                                   @Valid @RequestBody UpdateAdminUserRequest request) {
        userManagementService.updateUser(id, request);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @OperationLog(operation = "删除用户")
    @RequirePermission("user:delete")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userManagementService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "切换用户状态")
    @OperationLog(operation = "切换用户状态")
    @RequirePermission("user:status")
    @PutMapping("/{id}/status")
    public Result<Void> toggleUserStatus(@PathVariable Long id) {
        userManagementService.toggleUserStatus(id);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @OperationLog(operation = "重置用户密码")
    @RequirePermission("user:reset-password")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestBody @NotBlank(message = "��密码不能为空") String newPassword) {
        userManagementService.resetPassword(id, newPassword);
        return Result.success();
    }

    @Operation(summary = "分配用户角色")
    @OperationLog(operation = "分配用户角色")
    @RequirePermission("user:assign-role")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id,
                                    @RequestBody List<Long> roleIds) {
        userManagementService.assignRoles(id, roleIds);
        return Result.success();
    }
}
