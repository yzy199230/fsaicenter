package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模型查询请求
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "模型查询请求")
public class ModelQueryRequest {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "关键词（模型代码或名称）")
    private String keyword;

    @Schema(description = "模型类型", example = "CHAT")
    private String modelType;

    @Schema(description = "提供商ID", example = "1")
    private Long providerId;

    @Schema(description = "状态: ENABLED/DISABLED", example = "ENABLED")
    private String status;
}
