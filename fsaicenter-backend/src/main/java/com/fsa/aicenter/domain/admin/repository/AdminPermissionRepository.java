package com.fsa.aicenter.domain.admin.repository;

import com.fsa.aicenter.domain.admin.entity.AdminPermission;

import java.util.List;
import java.util.Optional;

/**
 * 权限仓储接口
 */
public interface AdminPermissionRepository {

    /**
     * 根据用户ID查询权限编码列表
     */
    List<String> findPermissionCodesByUserId(Long userId);

    /**
     * 根据用户ID查询权限列表
     */
    List<AdminPermission> findPermissionsByUserId(Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    List<AdminPermission> findPermissionsByRoleId(Long roleId);

    /**
     * 查询所有权限（平铺列表）
     */
    List<AdminPermission> findAll();

    /**
     * 查询所有权限树
     */
    List<AdminPermission> findAllTree();

    /**
     * 根据ID查询权限
     */
    Optional<AdminPermission> findById(Long id);

    /**
     * 保存权限
     */
    AdminPermission save(AdminPermission permission);

    /**
     * 更新权限
     */
    void update(AdminPermission permission);

    /**
     * 删除权限（逻辑删除）
     */
    void deleteById(Long id);
}
