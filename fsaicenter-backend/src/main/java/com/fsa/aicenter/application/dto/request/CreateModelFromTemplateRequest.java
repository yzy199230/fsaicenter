package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 从模板创建模型请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "从模板创建模型请求")
public class CreateModelFromTemplateRequest {

    @NotNull(message = "提供商ID不能为空")
    @Schema(description = "提供商ID", example = "1")
    private Long providerId;

    @NotEmpty(message = "模板代码列表不能为空")
    @Schema(description = "模板代码列表", example = "[\"gpt-4\", \"gpt-3.5-turbo\"]")
    private List<String> templateCodes;
}
