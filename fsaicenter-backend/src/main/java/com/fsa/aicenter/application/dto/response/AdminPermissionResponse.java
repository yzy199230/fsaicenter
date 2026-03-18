package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限响应
 */
@Data
@Schema(description = "权限响应")
public class AdminPermissionResponse {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "父权限ID")
    private Long parentId;

    @Schema(description = "权限编码")
    private String permissionCode;

    @Schema(description = "权限名称")
    private String permissionName;

    @Schema(description = "权限类型(MENU:菜单 BUTTON:按钮 API:接口)")
    private String permissionType;

    @Schema(description = "权限路径")
    private String permissionPath;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态(1:启用 0:禁用)")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "子权限列表")
    private List<AdminPermissionResponse> children;
}
