package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建管理员用户请求
 */
@Data
@Schema(description = "创建管理员用户请求")
public class CreateAdminUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50字符")
    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20字符")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Size(max = 50, message = "真实姓名最长50字符")
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "状态(1:启用 0:禁用)", example = "1")
    private Integer status = 1;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
