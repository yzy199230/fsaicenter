package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模板同步请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "模板同步请求")
public class TemplateSyncRequest {

    @NotBlank(message = "数据源不能为空")
    @Schema(description = "数据源名称", example = "litellm", requiredMode = Schema.RequiredMode.REQUIRED)
    private String source;

    @Schema(description = "是否为试运行（不实际写入数据库）", example = "false", defaultValue = "false")
    private Boolean dryRun = false;
}
