package com.fsa.aicenter.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fsa.aicenter.domain.admin.entity.AdminPermission;
import com.fsa.aicenter.domain.admin.repository.AdminPermissionRepository;
import com.fsa.aicenter.infrastructure.persistence.mapper.AdminPermissionMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminPermissionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 权限仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AdminPermissionRepositoryImpl implements AdminPermissionRepository {

    private final AdminPermissionMapper mapper;

    @Override
    public List<String> findPermissionCodesByUserId(Long userId) {
        return mapper.findPermissionCodesByUserId(userId);
    }

    @Override
    public List<AdminPermission> findPermissionsByUserId(Long userId) {
        List<AdminPermissionPO> pos = mapper.findPermissionsByUserId(userId);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AdminPermission> findPermissionsByRoleId(Long roleId) {
        List<AdminPermissionPO> pos = mapper.findPermissionsByRoleId(roleId);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AdminPermission> findAll() {
        List<AdminPermissionPO> pos = mapper.selectList(null);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<AdminPermission> findAllTree() {
        // 查询所有权限
        List<AdminPermissionPO> allPos = mapper.selectList(
                new LambdaQueryWrapper<AdminPermissionPO>()
                        .orderByAsc(AdminPermissionPO::getSortOrder)
        );

        // 转换为领域对象
        List<AdminPermission> allPermissions = allPos.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        // 构建树形结构
        return buildTree(allPermissions);
    }

    @Override
    public Optional<AdminPermission> findById(Long id) {
        AdminPermissionPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public AdminPermission save(AdminPermission permission) {
        AdminPermissionPO po = toPO(permission);
        mapper.insert(po);
        permission.setId(po.getId());
        return permission;
    }

    @Override
    public void update(AdminPermission permission) {
        AdminPermissionPO po = toPO(permission);
        mapper.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    /**
     * 构建权限树
     */
    private List<AdminPermission> buildTree(List<AdminPermission> allPermissions) {
        // 按父ID分组
        Map<Long, List<AdminPermission>> groupByParentId = allPermissions.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getParentId() != null ? p.getParentId() : 0L
                ));

        // 为每个权限设置子权限
        for (AdminPermission permission : allPermissions) {
            List<AdminPermission> children = groupByParentId.get(permission.getId());
            if (children != null && !children.isEmpty()) {
                permission.setChildren(children);
            }
        }

        // 返回顶级权限列表
        List<AdminPermission> topLevel = groupByParentId.get(0L);
        return topLevel != null ? topLevel : new ArrayList<>();
    }

    private AdminPermission toDomain(AdminPermissionPO po) {
        return AdminPermission.builder()
                .id(po.getId())
                .parentId(po.getParentId())
                .permissionCode(po.getPermissionCode())
                .permissionName(po.getPermissionName())
                .permissionType(po.getPermissionType())
                .permissionPath(po.getPermissionPath())
                .icon(po.getIcon())
                .sortOrder(po.getSortOrder())
                .status(po.getStatus())
                .createdTime(po.getCreatedTime())
                .updatedTime(po.getUpdatedTime())
                .isDeleted(po.getIsDeleted())
                .build();
    }

    private AdminPermissionPO toPO(AdminPermission permission) {
        AdminPermissionPO po = new AdminPermissionPO();
        po.setId(permission.getId());
        po.setParentId(permission.getParentId());
        po.setPermissionCode(permission.getPermissionCode());
        po.setPermissionName(permission.getPermissionName());
        po.setPermissionType(permission.getPermissionType());
        po.setPermissionPath(permission.getPermissionPath());
        po.setIcon(permission.getIcon());
        po.setSortOrder(permission.getSortOrder());
        po.setStatus(permission.getStatus());
        return po;
    }
}
