package com.fsa.aicenter.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fsa.aicenter.domain.admin.entity.AdminRole;
import com.fsa.aicenter.domain.admin.repository.AdminRoleRepository;
import com.fsa.aicenter.infrastructure.persistence.mapper.AdminRoleMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminRolePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 角色仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AdminRoleRepositoryImpl implements AdminRoleRepository {

    private final AdminRoleMapper mapper;

    @Override
    public List<String> findRoleCodesByUserId(Long userId) {
        return mapper.findRoleCodesByUserId(userId);
    }

    @Override
    public List<AdminRole> findRolesByUserId(Long userId) {
        List<AdminRolePO> pos = mapper.findRolesByUserId(userId);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<AdminRole> findById(Long id) {
        AdminRolePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<AdminRole> findByRoleCode(String roleCode) {
        LambdaQueryWrapper<AdminRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminRolePO::getRoleCode, roleCode);
        AdminRolePO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AdminRole> findAll() {
        List<AdminRolePO> pos = mapper.selectList(null);
        return pos.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public AdminRole save(AdminRole role) {
        AdminRolePO po = toPO(role);
        mapper.insert(po);
        role.setId(po.getId());
        return role;
    }

    @Override
    public void update(AdminRole role) {
        AdminRolePO po = toPO(role);
        mapper.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public boolean existsByRoleCodeAndIdNot(String roleCode, Long id) {
        LambdaQueryWrapper<AdminRolePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminRolePO::getRoleCode, roleCode)
               .ne(AdminRolePO::getId, id)
               .eq(AdminRolePO::getIsDeleted, 0);
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public void saveRolePermissions(Long roleId, List<Long> permissionIds) {
        if (permissionIds != null && !permissionIds.isEmpty()) {
            mapper.insertRolePermissions(roleId, permissionIds);
        }
    }

    @Override
    public void deleteRolePermissions(Long roleId) {
        mapper.deleteRolePermissions(roleId);
    }

    @Override
    public List<Long> findPermissionIdsByRoleId(Long roleId) {
        return mapper.findPermissionIdsByRoleId(roleId);
    }

    private AdminRole toDomain(AdminRolePO po) {
        return AdminRole.builder()
                .id(po.getId())
                .roleCode(po.getRoleCode())
                .roleName(po.getRoleName())
                .description(po.getDescription())
                .status(po.getStatus())
                .sortOrder(po.getSortOrder())
                .createdTime(po.getCreatedTime())
                .updatedTime(po.getUpdatedTime())
                .isDeleted(po.getIsDeleted())
                .build();
    }

    private AdminRolePO toPO(AdminRole role) {
        AdminRolePO po = new AdminRolePO();
        po.setId(role.getId());
        po.setRoleCode(role.getRoleCode());
        po.setRoleName(role.getRoleName());
        po.setDescription(role.getDescription());
        po.setStatus(role.getStatus());
        po.setSortOrder(role.getSortOrder());
        return po;
    }
}
