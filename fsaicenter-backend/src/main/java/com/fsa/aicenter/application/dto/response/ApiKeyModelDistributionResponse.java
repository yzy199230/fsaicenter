package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API密钥模型分布响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "API密钥模型分布响应")
public class ApiKeyModelDistributionResponse {

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型类型(CHAT/EMBEDDING/IMAGE)")
    private String modelType;

    @Schema(description = "请求数")
    private Long requests;

    @Schema(description = "占比(%)")
    private Double percentage;
}
