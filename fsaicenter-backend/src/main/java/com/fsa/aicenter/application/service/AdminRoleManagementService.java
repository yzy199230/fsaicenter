package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.request.CreateAdminRoleRequest;
import com.fsa.aicenter.application.dto.request.UpdateAdminRoleRequest;
import com.fsa.aicenter.application.dto.response.AdminRoleResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.common.model.PageResult;
import com.fsa.aicenter.domain.admin.entity.AdminRole;
import com.fsa.aicenter.domain.admin.repository.AdminRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleManagementService {

    private final AdminRoleRepository roleRepository;

    /**
     * 查询所有角色（下拉框用）
     */
    public List<AdminRoleResponse> listAllRoles() {
        return roleRepository.findAll().stream()
                .filter(role -> role.getIsDeleted() == null || role.getIsDeleted() == 0)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询角色
     */
    public PageResult<AdminRoleResponse> listRoles(String keyword, Integer status, int pageNum, int pageSize) {
        List<AdminRole> roles = roleRepository.findAll().stream()
                .filter(role -> role.getIsDeleted() == null || role.getIsDeleted() == 0)
                .filter(role -> keyword == null || keyword.isEmpty() ||
                        role.getRoleCode().contains(keyword) || role.getRoleName().contains(keyword))
                .filter(role -> status == null || role.getStatus().equals(status))
                .collect(Collectors.toList());

        List<AdminRoleResponse> responses = roles.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // 计算分页
        int total = responses.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        List<AdminRoleResponse> pageData = start < total ? responses.subList(start, end) : List.of();

        return PageResult.of((long) total, pageNum, pageSize, pageData);
    }

    /**
     * 获取角色详情
     */
    public AdminRoleResponse getRole(Long id) {
        AdminRole role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        return toResponse(role);
    }

    /**
     * 创建角色
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(CreateAdminRoleRequest request) {
        // 检查角色编码是否存在
        if (roleRepository.findByRoleCode(request.getRoleCode()).isPresent()) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }

        // 构建角色
        AdminRole role = AdminRole.builder()
                .roleCode(request.getRoleCode())
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .isDeleted(0)
                .build();

        // 保存角色
        AdminRole savedRole = roleRepository.save(role);

        // 保存角色权限关联
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            roleRepository.saveRolePermissions(savedRole.getId(), request.getPermissionIds());
        }

        log.info("创建角色成功: id={}, roleCode={}", savedRole.getId(), savedRole.getRoleCode());
        return savedRole.getId();
    }

    /**
     * 更新角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, UpdateAdminRoleRequest request) {
        AdminRole role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        // 不能修改超级管理员角色编码
        if ("SUPER_ADMIN".equals(role.getRoleCode())) {
            // 超级管理员只能修改描述
            if (request.getDescription() != null) {
                role.setDescription(request.getDescription());
            }
        } else {
            // 普通角色可以修改所有字段
            if (request.getRoleName() != null) {
                role.setRoleName(request.getRoleName());
            }
            if (request.getDescription() != null) {
                role.setDescription(request.getDescription());
            }
            if (request.getSortOrder() != null) {
                role.setSortOrder(request.getSortOrder());
            }
            if (request.getStatus() != null) {
                role.setStatus(request.getStatus());
            }
        }

        roleRepository.update(role);

        // 更新权限关联（超级管理员不更新权限）
        if (!"SUPER_ADMIN".equals(role.getRoleCode()) && request.getPermissionIds() != null) {
            roleRepository.deleteRolePermissions(id);
            if (!request.getPermissionIds().isEmpty()) {
                roleRepository.saveRolePermissions(id, request.getPermissionIds());
            }
        }

        log.info("更新角色成功: id={}, roleCode={}", role.getId(), role.getRoleCode());
    }

    /**
     * 删除角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        AdminRole role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        // 不能删除系统角色
        if ("SUPER_ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.OPERATION_FORBIDDEN, "不能删除超级管理员角色");
        }

        // 删除角色权限关联
        roleRepository.deleteRolePermissions(id);
        // 删除角色
        roleRepository.deleteById(id);

        log.info("删除角色成功: id={}, roleCode={}", role.getId(), role.getRoleCode());
    }

    /**
     * 切换角色状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleRoleStatus(Long id) {
        AdminRole role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        // 不能禁用超级管理员角色
        if ("SUPER_ADMIN".equals(role.getRoleCode()) && role.getStatus() == 1) {
            throw new BusinessException(ErrorCode.OPERATION_FORBIDDEN, "不能禁用超级管理员角色");
        }

        role.setStatus(role.getStatus() == 1 ? 0 : 1);
        roleRepository.update(role);

        log.info("切换角色状态成功: id={}, status={}", role.getId(), role.getStatus());
    }

    /**
     * 分配权限
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        AdminRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        // 超级管理员不能修改权限
        if ("SUPER_ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.OPERATION_FORBIDDEN, "不能修改超级管理员角色的权限");
        }

        // 先删除原有权限
        roleRepository.deleteRolePermissions(roleId);

        // 保存新权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            roleRepository.saveRolePermissions(roleId, permissionIds);
        }

        log.info("分配角色权限成功: roleId={}, permissionIds={}", roleId, permissionIds);
    }

    /**
     * 转换为响应
     */
    private AdminRoleResponse toResponse(AdminRole role) {
        AdminRoleResponse response = new AdminRoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setSortOrder(role.getSortOrder());
        response.setStatus(role.getStatus());
        response.setCreatedTime(role.getCreatedTime());
        response.setUpdatedTime(role.getUpdatedTime());

        // 获取权限ID列表
        List<Long> permissionIds = roleRepository.findPermissionIdsByRoleId(role.getId());
        response.setPermissionIds(permissionIds);

        return response;
    }
}
