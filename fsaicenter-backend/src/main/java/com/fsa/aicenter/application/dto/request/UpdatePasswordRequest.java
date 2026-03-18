package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "修改密码请求")
public class UpdatePasswordRequest {

    @Schema(description = "当前密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "当前密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50个字符之间")
    private String newPassword;
}
