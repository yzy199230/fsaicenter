package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API密钥请求日志响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "API密钥请求日志响应")
public class ApiKeyLogResponse {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型类型")
    private String modelType;

    @Schema(description = "状态(SUCCESS/FAILED)")
    private String status;

    @Schema(description = "提示Token数")
    private Long promptTokens;

    @Schema(description = "完成Token数")
    private Long completionTokens;

    @Schema(description = "总Token数")
    private Long totalTokens;

    @Schema(description = "响应时间(ms)")
    private Long responseTime;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "请求时间")
    private LocalDateTime requestTime;
}
