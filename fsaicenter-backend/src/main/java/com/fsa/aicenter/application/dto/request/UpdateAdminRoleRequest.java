package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新角色请求
 */
@Data
@Schema(description = "更新角色请求")
public class UpdateAdminRoleRequest {

    @Size(max = 50, message = "角色名称最长50字符")
    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    @Size(max = 200, message = "描述最长200字符")
    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态(1:启用 0:禁用)")
    private Integer status;

    @Schema(description = "权限ID列表")
    private List<Long> permissionIds;
}
