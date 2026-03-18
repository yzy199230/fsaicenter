package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminPermissionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface AdminPermissionMapper extends BaseMapper<AdminPermissionPO> {

    /**
     * 根据用户ID查询权限编码列表
     */
    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询权限列表
     */
    List<AdminPermissionPO> findPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    List<AdminPermissionPO> findPermissionsByRoleId(@Param("roleId") Long roleId);
}
