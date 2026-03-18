package com.fsa.aicenter.domain.admin.repository;

import com.fsa.aicenter.domain.admin.aggregate.AdminUser;

import java.util.List;
import java.util.Optional;

/**
 * 管理员用户仓储接口
 *
 * @author FSA AI Center
 */
public interface AdminUserRepository {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<AdminUser> findByUsername(String username);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    Optional<AdminUser> findById(Long id);

    /**
     * 保存用户
     *
     * @param user 用户信息
     * @return 保存后的用户
     */
    AdminUser save(AdminUser user);

    /**
     * 更新用户
     *
     * @param user 用户信息
     * @return 更新后的用户
     */
    AdminUser update(AdminUser user);

    /**
     * 更新最后登录信息
     *
     * @param userId 用户ID
     * @param ip     登录IP
     */
    void updateLoginInfo(Long userId, String ip);

    /**
     * 分页查询用户
     */
    List<AdminUser> findByCondition(String keyword, Integer status);

    /**
     * 逻辑删除用户
     */
    void deleteById(Long id);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查用户名是否存在（排除指定ID）
     */
    boolean existsByUsernameAndIdNot(String username, Long id);

    /**
     * 查询用户角色ID列表
     */
    List<Long> findRoleIdsByUserId(Long userId);

    /**
     * 保存用户角色关联
     */
    void saveUserRoles(Long userId, List<Long> roleIds);

    /**
     * 删除用户角色关联
     */
    void deleteUserRoles(Long userId);
}
