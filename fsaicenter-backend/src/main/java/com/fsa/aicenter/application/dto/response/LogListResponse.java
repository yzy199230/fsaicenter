package com.fsa.aicenter.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "日志列表响应")
public class LogListResponse {
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "请求ID")
    private String requestId;

    @Schema(description = "API密钥ID")
    private Long apiKeyId;

    @Schema(description = "API密钥名称")
    private String apiKeyName;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型编码")
    private String modelCode;

    @Schema(description = "模型类型")
    private String modelType;

    @Schema(description = "请求时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;

    @Schema(description = "响应时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseTime;

    @Schema(description = "耗时(ms)")
    private Integer duration;

    @Schema(description = "输入Token数")
    private Integer inputTokens;

    @Schema(description = "输出Token数")
    private Integer outputTokens;

    @Schema(description = "状态(SUCCESS/FAILED)")
    private String status;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "UserAgent")
    private String userAgent;
}
