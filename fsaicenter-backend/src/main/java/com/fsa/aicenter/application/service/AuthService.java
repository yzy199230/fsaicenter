package com.fsa.aicenter.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fsa.aicenter.application.dto.request.LoginRequest;
import com.fsa.aicenter.application.dto.response.LoginResponse;
import com.fsa.aicenter.application.dto.response.UserInfoResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.admin.aggregate.AdminUser;
import com.fsa.aicenter.domain.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @param ip      登录IP
     * @return 登录响应
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request, String ip) {
        log.info("用户登录: username={}, ip={}", request.getUsername(), ip);

        // 查询用户
        AdminUser user = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 验证密码（应用层直接验证）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("用户密码错误: username={}", request.getUsername());
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 检查用户状态
        if (!user.isEnabled()) {
            log.warn("用户已被禁用: username={}", request.getUsername());
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 更新登录信息
        adminUserRepository.updateLoginInfo(user.getId(), ip);

        // 执行登录，生成token
        StpUtil.login(user.getId());

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .tokenName(StpUtil.getTokenName())
                .tokenValue(StpUtil.getTokenValue())
                .build();
    }

    /**
     * 用户登出
     */
    public void logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("用户登出成功: userId={}", userId);
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    public UserInfoResponse getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();

        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .build();
    }
}
