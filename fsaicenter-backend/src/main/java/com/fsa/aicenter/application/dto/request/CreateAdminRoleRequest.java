package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建角色请求
 */
@Data
@Schema(description = "创建角色请求")
public class CreateAdminRoleRequest {

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码最长50字符")
    @Schema(description = "角色编码", example = "ADMIN")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称最长50字符")
    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    @Size(max = 200, message = "描述最长200字符")
    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder = 0;

    @Schema(description = "状态(1:启用 0:禁用)", example = "1")
    private Integer status = 1;

    @Schema(description = "权限ID列表")
    private List<Long> permissionIds;
}
