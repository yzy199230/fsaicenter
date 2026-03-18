package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新管理员用户请求
 */
@Data
@Schema(description = "更新管理员用户请求")
public class UpdateAdminUserRequest {

    @Size(max = 50, message = "真实姓名最长50字符")
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "状态(1:启用 0:禁用)", example = "1")
    private Integer status;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
