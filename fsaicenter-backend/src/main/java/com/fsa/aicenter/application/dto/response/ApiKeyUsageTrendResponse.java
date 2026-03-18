package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API密钥使用趋势响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "API密钥使用趋势响应")
public class ApiKeyUsageTrendResponse {

    @Schema(description = "日期(MM-dd)")
    private String date;

    @Schema(description = "请求数")
    private Long requests;

    @Schema(description = "Token数")
    private Long tokens;

    @Schema(description = "成功数")
    private Long successCount;

    @Schema(description = "失败数")
    private Long failedCount;
}
