package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.response.AdminPermissionResponse;
import com.fsa.aicenter.domain.admin.entity.AdminPermission;
import com.fsa.aicenter.domain.admin.repository.AdminPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPermissionManagementService {

    private final AdminPermissionRepository permissionRepository;

    /**
     * 获取权限树
     */
    public List<AdminPermissionResponse> getPermissionTree() {
        List<AdminPermission> allPermissions = permissionRepository.findAll();
        return buildTree(allPermissions);
    }

    /**
     * 获取所有权限（平铺）
     */
    public List<AdminPermissionResponse> listAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色ID获取权限
     */
    public List<AdminPermissionResponse> getPermissionsByRoleId(Long roleId) {
        return permissionRepository.findPermissionsByRoleId(roleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 构建权限树
     */
    private List<AdminPermissionResponse> buildTree(List<AdminPermission> permissions) {
        // 转换为响应对象
        List<AdminPermissionResponse> responses = permissions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // 构建ID到对象的映射
        Map<Long, AdminPermissionResponse> map = responses.stream()
                .collect(Collectors.toMap(AdminPermissionResponse::getId, r -> r));

        List<AdminPermissionResponse> tree = new ArrayList<>();

        for (AdminPermissionResponse response : responses) {
            Long parentId = response.getParentId();
            if (parentId == null || parentId == 0) {
                // 顶级节点
                tree.add(response);
            } else {
                // 找到父节点并添加到其children中
                AdminPermissionResponse parent = map.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(response);
                }
            }
        }

        return tree;
    }

    /**
     * 转换为响应
     */
    private AdminPermissionResponse toResponse(AdminPermission permission) {
        AdminPermissionResponse response = new AdminPermissionResponse();
        response.setId(permission.getId());
        response.setParentId(permission.getParentId());
        response.setPermissionCode(permission.getPermissionCode());
        response.setPermissionName(permission.getPermissionName());
        response.setPermissionType(permission.getPermissionType());
        response.setPermissionPath(permission.getPermissionPath());
        response.setIcon(permission.getIcon());
        response.setSortOrder(permission.getSortOrder());
        response.setStatus(permission.getStatus());
        response.setCreatedTime(permission.getCreatedTime());
        return response;
    }
}
