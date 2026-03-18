package com.fsa.aicenter.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "日志列表查询参数")
public class LogListQuery {
    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "关键词(搜索requestId/apiKey/model)")
    private String keyword;

    @Schema(description = "模型类型(CHAT/EMBEDDING/IMAGE)")
    private String modelType;

    @Schema(description = "状态(SUCCESS/FAILED)")
    private String status;

    @Schema(description = "开始时间(yyyy-MM-dd HH:mm:ss)")
    private String startTime;

    @Schema(description = "结束时间(yyyy-MM-dd HH:mm:ss)")
    private String endTime;
}
