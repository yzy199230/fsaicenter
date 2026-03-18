package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户查询请求
 */
@Data
@Schema(description = "用户查询请求")
public class AdminUserQueryRequest {

    @Schema(description = "关键词(用户名/真实姓名)")
    private String keyword;

    @Schema(description = "状态(1:启用 0:禁用)")
    private Integer status;

    @Min(1)
    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Min(1)
    @Max(100)
    @Schema(description = "每页条数", defaultValue = "10")
    private Integer pageSize = 10;
}
