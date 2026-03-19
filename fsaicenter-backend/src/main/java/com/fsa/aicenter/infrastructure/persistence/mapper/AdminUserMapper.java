package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminUserPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 管理员用户Mapper接口
 *
 * @author FSA AI Center
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUserPO> {

    @Update("UPDATE admin_user SET last_login_time = CURRENT_TIMESTAMP, last_login_ip = #{ip}, updated_time = CURRENT_TIMESTAMP WHERE id = #{userId} AND is_deleted = 0")
    void updateLoginInfo(@Param("userId") Long userId, @Param("ip") String ip);

    @Insert("<script>INSERT INTO admin_user_role (user_id, role_id) VALUES " +
            "<foreach collection='roleIds' item='roleId' separator=','>(#{userId}, #{roleId})</foreach></script>")
    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    @Update("UPDATE admin_user_role SET is_deleted = 1 WHERE user_id = #{userId} AND is_deleted = 0")
    void deleteUserRoles(@Param("userId") Long userId);

    @Select("SELECT role_id FROM admin_user_role WHERE user_id = #{userId} AND is_deleted = 0")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);
}
