package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "日志详情响应")
public class LogDetailResponse extends LogListResponse {
    @Schema(description = "请求体")
    private String requestBody;

    @Schema(description = "响应体")
    private String responseBody;
}
