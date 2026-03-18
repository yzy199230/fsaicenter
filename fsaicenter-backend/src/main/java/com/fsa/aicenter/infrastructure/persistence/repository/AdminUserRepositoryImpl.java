package com.fsa.aicenter.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fsa.aicenter.domain.admin.aggregate.AdminUser;
import com.fsa.aicenter.domain.admin.repository.AdminUserRepository;
import com.fsa.aicenter.infrastructure.persistence.mapper.AdminUserMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminUserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 管理员用户仓储实现
 *
 * @author FSA AI Center
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AdminUserRepositoryImpl implements AdminUserRepository {

    private final AdminUserMapper adminUserMapper;

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        LambdaQueryWrapper<AdminUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminUserPO::getUsername, username);

        AdminUserPO po = adminUserMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<AdminUser> findById(Long id) {
        AdminUserPO po = adminUserMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public AdminUser save(AdminUser user) {
        AdminUserPO po = toPO(user);
        po.setCreatedTime(LocalDateTime.now());
        po.setUpdatedTime(LocalDateTime.now());
        adminUserMapper.insert(po);
        return toDomain(po);
    }

    @Override
    public AdminUser update(AdminUser user) {
        AdminUserPO po = toPO(user);
        po.setUpdatedTime(LocalDateTime.now());
        adminUserMapper.updateById(po);
        return toDomain(po);
    }

    @Override
    public void updateLoginInfo(Long userId, String ip) {
        adminUserMapper.updateLoginInfo(userId, ip);
    }

    @Override
    public List<AdminUser> findByCondition(String keyword, Integer status) {
        LambdaQueryWrapper<AdminUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminUserPO::getIsDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(AdminUserPO::getUsername, keyword)
                    .or().like(AdminUserPO::getRealName, keyword));
        }
        if (status != null) {
            wrapper.eq(AdminUserPO::getStatus, status);
        }
        wrapper.orderByDesc(AdminUserPO::getCreatedTime);
        return adminUserMapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        AdminUserPO po = new AdminUserPO();
        po.setId(id);
        po.setIsDeleted(1);
        po.setUpdatedTime(LocalDateTime.now());
        adminUserMapper.updateById(po);
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<AdminUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminUserPO::getUsername, username).eq(AdminUserPO::getIsDeleted, 0);
        return adminUserMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByUsernameAndIdNot(String username, Long id) {
        LambdaQueryWrapper<AdminUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminUserPO::getUsername, username).ne(AdminUserPO::getId, id).eq(AdminUserPO::getIsDeleted, 0);
        return adminUserMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        return adminUserMapper.findRoleIdsByUserId(userId);
    }

    @Override
    public void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            adminUserMapper.insertUserRoles(userId, roleIds);
        }
    }

    @Override
    public void deleteUserRoles(Long userId) {
        adminUserMapper.deleteUserRoles(userId);
    }

    /**
     * PO转领域对象
     */
    private AdminUser toDomain(AdminUserPO po) {
        return AdminUser.builder()
                .id(po.getId())
                .username(po.getUsername())
                .password(po.getPassword())
                .realName(po.getRealName())
                .email(po.getEmail())
                .phone(po.getPhone())
                .avatar(po.getAvatar())
                .status(po.getStatus())
                .lastLoginTime(po.getLastLoginTime())
                .lastLoginIp(po.getLastLoginIp())
                .createdTime(po.getCreatedTime())
                .updatedTime(po.getUpdatedTime())
                .isDeleted(po.getIsDeleted())
                .build();
    }

    /**
     * 领域对象转PO
     */
    private AdminUserPO toPO(AdminUser user) {
        return AdminUserPO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .createdTime(user.getCreatedTime())
                .updatedTime(user.getUpdatedTime())
                .isDeleted(user.getIsDeleted())
                .build();
    }
}
