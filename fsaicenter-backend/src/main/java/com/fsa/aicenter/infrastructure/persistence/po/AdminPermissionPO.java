package com.fsa.aicenter.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限PO
 */
@Data
@TableName("admin_permission")
public class AdminPermissionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private String permissionPath;
    private String icon;
    private Integer sortOrder;
    private Integer status;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer isDeleted;
}
