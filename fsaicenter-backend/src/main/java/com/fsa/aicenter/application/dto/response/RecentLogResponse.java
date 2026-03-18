package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 最近请求日志响应
 */
@Data
@Schema(description = "最近请求日志")
public class RecentLogResponse {

    @Schema(description = "请求时间")
    private String time;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "API密钥名称")
    private String apiKey;

    @Schema(description = "状态(1=成功,0=失败)")
    private Integer status;

    @Schema(description = "耗时")
    private String duration;
}
