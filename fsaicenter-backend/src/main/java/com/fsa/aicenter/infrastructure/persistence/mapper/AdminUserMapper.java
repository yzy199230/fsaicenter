package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminUserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理员用户Mapper接口
 *
 * @author FSA AI Center
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUserPO> {

    /**
     * 更新最后登录信息
     *
     * @param userId 用户ID
     * @param ip     登录IP
     */
    void updateLoginInfo(@Param("userId") Long userId, @Param("ip") String ip);

    /**
     * 保存用户角色关联
     */
    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 删除用户角色关联
     */
    void deleteUserRoles(@Param("userId") Long userId);

    /**
     * 查询用户角色ID列表
     */
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);
}
