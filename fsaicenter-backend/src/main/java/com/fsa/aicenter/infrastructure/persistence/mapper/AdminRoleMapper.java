package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminRolePO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRolePO> {

    @Select("SELECT r.role_code FROM admin_role r " +
            "JOIN admin_user_role ur ON r.id = ur.role_id AND ur.is_deleted = 0 " +
            "WHERE ur.user_id = #{userId} AND r.is_deleted = 0 AND r.status = 1")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT r.* FROM admin_role r " +
            "JOIN admin_user_role ur ON r.id = ur.role_id AND ur.is_deleted = 0 " +
            "WHERE ur.user_id = #{userId} AND r.is_deleted = 0 AND r.status = 1")
    List<AdminRolePO> findRolesByUserId(@Param("userId") Long userId);

    @Insert("<script>INSERT INTO admin_role_permission (role_id, permission_id) VALUES " +
            "<foreach collection='permissionIds' item='permId' separator=','>(#{roleId}, #{permId})</foreach></script>")
    void insertRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    @Update("UPDATE admin_role_permission SET is_deleted = 1 WHERE role_id = #{roleId} AND is_deleted = 0")
    void deleteRolePermissions(@Param("roleId") Long roleId);

    @Select("SELECT permission_id FROM admin_role_permission WHERE role_id = #{roleId} AND is_deleted = 0")
    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
