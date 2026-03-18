package com.fsa.aicenter.domain.admin.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPermission {

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 父权限ID(0表示顶级)
     */
    private Long parentId;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限类型(MENU:菜单 BUTTON:按钮 API:接口)
     */
    private String permissionType;

    /**
     * 权限路径(菜单路径或API路径)
     */
    private String permissionPath;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态(1:启用 0:禁用)
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 删除标记
     */
    private Integer isDeleted;

    /**
     * 子权限列表（用于权限树）
     */
    private List<AdminPermission> children;

    /**
     * 权限类型枚举
     */
    public enum PermissionType {
        MENU("MENU", "菜单"),
        BUTTON("BUTTON", "按钮"),
        API("API", "接口");

        private final String code;
        private final String desc;

        PermissionType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    /**
     * 判断权限是否启用
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    /**
     * 判断是否为顶级权限
     */
    public boolean isTopLevel() {
        return this.parentId == null || this.parentId == 0;
    }
}
