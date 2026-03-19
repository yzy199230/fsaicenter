package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminPermissionPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface AdminPermissionMapper extends BaseMapper<AdminPermissionPO> {

    @Select("SELECT DISTINCT p.permission_code FROM admin_permission p " +
            "JOIN admin_role_permission rp ON p.id = rp.permission_id AND rp.is_deleted = 0 " +
            "JOIN admin_user_role ur ON rp.role_id = ur.role_id AND ur.is_deleted = 0 " +
            "WHERE ur.user_id = #{userId} AND p.is_deleted = 0 AND p.status = 1")
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT p.* FROM admin_permission p " +
            "JOIN admin_role_permission rp ON p.id = rp.permission_id AND rp.is_deleted = 0 " +
            "JOIN admin_user_role ur ON rp.role_id = ur.role_id AND ur.is_deleted = 0 " +
            "WHERE ur.user_id = #{userId} AND p.is_deleted = 0 AND p.status = 1")
    List<AdminPermissionPO> findPermissionsByUserId(@Param("userId") Long userId);

    @Select("SELECT p.* FROM admin_permission p " +
            "JOIN admin_role_permission rp ON p.id = rp.permission_id AND rp.is_deleted = 0 " +
            "WHERE rp.role_id = #{roleId} AND p.is_deleted = 0 AND p.status = 1")
    List<AdminPermissionPO> findPermissionsByRoleId(@Param("roleId") Long roleId);
}
