package com.fsa.aicenter.interfaces.admin.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fsa.aicenter.application.dto.request.UpdatePasswordRequest;
import com.fsa.aicenter.application.dto.request.UpdateUserInfoRequest;
import com.fsa.aicenter.application.service.UserService;
import com.fsa.aicenter.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 *
 * @author FSA AI Center
 */
@Slf4j
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户个人信息管理接口")
public class UserController {

    private final UserService userService;

    /**
     * 更新用户基本信息
     */
    @PutMapping("/info")
    @SaCheckLogin
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的基本信息")
    public Result<Void> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        userService.updateUserInfo(request);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @SaCheckLogin
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(request);
        return Result.success();
    }
}
