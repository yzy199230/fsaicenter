package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 同步数据源响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "同步数据源信息")
public class SyncSourceResponse {

    @Schema(description = "数据源名称（用于API调用）", example = "litellm")
    private String name;

    @Schema(description = "显示名称", example = "LiteLLM 模型库")
    private String displayName;

    @Schema(description = "是否可用")
    private boolean available;
}
