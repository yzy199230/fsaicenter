package com.fsa.aicenter.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fsa.aicenter.application.dto.request.UpdatePasswordRequest;
import com.fsa.aicenter.application.dto.request.UpdateUserInfoRequest;
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
 * 用户管理服务
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 更新用户基本信息
     *
     * @param request 更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UpdateUserInfoRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("更新用户信息: userId={}", userId);

        AdminUser user = adminUserRepository.findById(userId)
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

        adminUserRepository.update(user);
        log.info("用户信息更新成功: userId={}", userId);
    }

    /**
     * 修改密码
     *
     * @param request 修改密码请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UpdatePasswordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("修改密码: userId={}", userId);

        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("旧密码验证失败: userId={}", userId);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "当前密码错误");
        }

        // 检查新密码是否与旧密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能与当前密码相同");
        }

        // 加密新密码并更新
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.update(user);

        log.info("密码修改成功: userId={}", userId);
    }
}
