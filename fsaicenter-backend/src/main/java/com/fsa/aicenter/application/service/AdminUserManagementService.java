package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.request.AdminUserQueryRequest;
import com.fsa.aicenter.application.dto.request.CreateAdminUserRequest;
import com.fsa.aicenter.application.dto.request.UpdateAdminUserRequest;
import com.fsa.aicenter.application.dto.response.AdminRoleResponse;
import com.fsa.aicenter.application.dto.response.AdminUserResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.common.model.PageResult;
import com.fsa.aicenter.domain.admin.aggregate.AdminUser;
import com.fsa.aicenter.domain.admin.entity.AdminRole;
import com.fsa.aicenter.domain.admin.repository.AdminRoleRepository;
import com.fsa.aicenter.domain.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private final AdminUserRepository userRepository;
    private final AdminRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户
     */
    public PageResult<AdminUserResponse> listUsers(AdminUserQueryRequest request) {
        List<AdminUser> users = userRepository.findByCondition(request.getKeyword(), request.getStatus());

        // 转换为响应并填充角色信息
        List<AdminUserResponse> responses = users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // 计算分页
        int total = responses.size();
        int start = (request.getPageNum() - 1) * request.getPageSize();
        int end = Math.min(start + request.getPageSize(), total);

        List<AdminUserResponse> pageData = start < total ? responses.subList(start, end) : List.of();

        return PageResult.of((long) total, request.getPageNum(), request.getPageSize(), pageData);
    }

    /**
     * 获取用户详情
     */
    public AdminUserResponse getUser(Long id) {
        AdminUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return toResponse(user);
    }

    /**
     * 创建用户
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(CreateAdminUserRequest request) {
        // 检查用户名是否存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 构建用户
        AdminUser user = AdminUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .realName(request.getRealName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .isDeleted(0)
                .build();

        // 保存用户
        AdminUser savedUser = userRepository.save(user);

        // 保存用户角色关联
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            userRepository.saveUserRoles(savedUser.getId(), request.getRoleIds());
        }

        log.info("创建用户成功: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        return savedUser.getId();
    }

    /**
     * 更新用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UpdateAdminUserRequest request) {
        AdminUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 更新字段
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userRepository.update(user);

        // 更新角色关联
        if (request.getRoleIds() != null) {
            userRepository.deleteUserRoles(id);
            if (!request.getRoleIds().isEmpty()) {
                userRepository.saveUserRoles(id, request.getRoleIds());
            }
        }

        log.info("更新用户成功: id={}, username={}", user.getId(), user.getUsername());
    }

    /**
     * 删除用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        AdminUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 不能删除超级管理员
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException(ErrorCode.OPERATION_FORBIDDEN, "不能删除超级管理员");
        }

        // 删除用户角色关联
        userRepository.deleteUserRoles(id);
        // 逻辑删除用户
        userRepository.deleteById(id);

        log.info("删除用户成功: id={}, username={}", user.getId(), user.getUsername());
    }

    /**
     * 切换用户状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleUserStatus(Long id) {
        AdminUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 不能禁用超级管理员
        if ("admin".equals(user.getUsername()) && user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.OPERATION_FORBIDDEN, "不能禁用超级管理员");
        }

        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        userRepository.update(user);

        log.info("切换用户状态成功: id={}, status={}", user.getId(), user.getStatus());
    }

    /**
     * 重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        AdminUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.update(user);

        log.info("重置用户密码成功: id={}, username={}", user.getId(), user.getUsername());
    }

    /**
     * 分配角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 先删除原有角色
        userRepository.deleteUserRoles(userId);

        // 保存新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            userRepository.saveUserRoles(userId, roleIds);
        }

        log.info("分配用户角色成功: userId={}, roleIds={}", userId, roleIds);
    }

    /**
     * 转换为响应
     */
    private AdminUserResponse toResponse(AdminUser user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatar(user.getAvatar());
        response.setStatus(user.getStatus());
        response.setLastLoginTime(user.getLastLoginTime());
        response.setLastLoginIp(user.getLastLoginIp());
        response.setCreatedTime(user.getCreatedTime());
        response.setUpdatedTime(user.getUpdatedTime());

        // 获取角色信息
        List<AdminRole> roles = roleRepository.findRolesByUserId(user.getId());
        response.setRoles(roles.stream().map(this::toRoleResponse).collect(Collectors.toList()));
        response.setRoleIds(roles.stream().map(AdminRole::getId).collect(Collectors.toList()));

        return response;
    }

    private AdminRoleResponse toRoleResponse(AdminRole role) {
        AdminRoleResponse response = new AdminRoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setSortOrder(role.getSortOrder());
        response.setStatus(role.getStatus());
        response.setCreatedTime(role.getCreatedTime());
        response.setUpdatedTime(role.getUpdatedTime());
        return response;
    }
}
