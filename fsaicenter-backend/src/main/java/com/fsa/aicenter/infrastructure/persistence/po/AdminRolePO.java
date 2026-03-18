package com.fsa.aicenter.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色PO
 */
@Data
@TableName("admin_role")
public class AdminRolePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private Integer sortOrder;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer isDeleted;
}
