package com.fsa.aicenter.domain.admin.repository;

import com.fsa.aicenter.domain.admin.entity.AdminRole;

import java.util.List;
import java.util.Optional;

/**
 * 角色仓储接口
 */
public interface AdminRoleRepository {

    /**
     * 根据用户ID查询角色编码列表
     */
    List<String> findRoleCodesByUserId(Long userId);

    /**
     * 根据用户ID查询角色列表
     */
    List<AdminRole> findRolesByUserId(Long userId);

    /**
     * 根据ID查询角色
     */
    Optional<AdminRole> findById(Long id);

    /**
     * 根据角色编码查��角色
     */
    Optional<AdminRole> findByRoleCode(String roleCode);

    /**
     * 查询所有角色
     */
    List<AdminRole> findAll();

    /**
     * 保存角色
     */
    AdminRole save(AdminRole role);

    /**
     * 更新角色
     */
    void update(AdminRole role);

    /**
     * 删除角色（逻辑删除）
     */
    void deleteById(Long id);

    /**
     * 检查角色编码是否存在（排除指定ID）
     */
    boolean existsByRoleCodeAndIdNot(String roleCode, Long id);

    /**
     * 保存角色权限关联
     */
    void saveRolePermissions(Long roleId, List<Long> permissionIds);

    /**
     * 删除角色权限关联
     */
    void deleteRolePermissions(Long roleId);

    /**
     * 查询角色权限ID列表
     */
    List<Long> findPermissionIdsByRoleId(Long roleId);
}
