package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "更新用户信息请求")
public class UpdateUserInfoRequest {

    @Schema(description = "真实姓名")
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;
}
