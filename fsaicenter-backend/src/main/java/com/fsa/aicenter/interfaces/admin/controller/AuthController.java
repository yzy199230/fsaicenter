package com.fsa.aicenter.interfaces.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fsa.aicenter.application.dto.request.LoginRequest;
import com.fsa.aicenter.application.dto.response.LoginResponse;
import com.fsa.aicenter.application.dto.response.UserInfoResponse;
import com.fsa.aicenter.application.service.AuthService;
import com.fsa.aicenter.common.model.Result;
import com.fsa.aicenter.infrastructure.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author FSA AI Center
 */
@Slf4j
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "管理后台认证相关接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "管理员用户登录接口")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = IpUtil.getClientIp(httpRequest);
        LoginResponse response = authService.login(request, ip);
        return Result.success(response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @SaCheckLogin
    @Operation(summary = "用户登出", description = "退出登录")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    @SaCheckLogin
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<UserInfoResponse> getCurrentUser() {
        UserInfoResponse response = authService.getCurrentUser();
        return Result.success(response);
    }
}
