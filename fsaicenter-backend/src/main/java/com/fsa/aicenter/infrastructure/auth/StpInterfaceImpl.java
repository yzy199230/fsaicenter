package com.fsa.aicenter.infrastructure.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.fsa.aicenter.domain.admin.repository.AdminPermissionRepository;
import com.fsa.aicenter.domain.admin.repository.AdminRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * sa-token权限接口实现
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final AdminRoleRepository roleRepository;
    private final AdminPermissionRepository permissionRepository;

    /**
     * 返回指定用户拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        return permissionRepository.findPermissionCodesByUserId(userId);
    }

    /**
     * 返回指定用户拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        return roleRepository.findRoleCodesByUserId(userId);
    }
}
