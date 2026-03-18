package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminRolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRolePO> {

    /**
     * 根据用户ID查询角色编码列表
     */
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询角色列表
     */
    List<AdminRolePO> findRolesByUserId(@Param("userId") Long userId);

    /**
     * 保存角色权限关联
     */
    void insertRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    /**
     * 删除角色权限关联
     */
    void deleteRolePermissions(@Param("roleId") Long roleId);

    /**
     * 查询角色权限ID列表
     */
    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
