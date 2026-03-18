package com.fsa.aicenter.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板同步响应
 *
 * @author FSA AI Center
 */
@Data
@Schema(description = "模板同步响应")
public class TemplateSyncResponse {

    @Schema(description = "数据源名称")
    private String sourceName;

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "新增模板数量")
    private int addedCount;

    @Schema(description = "更新模板数量")
    private int updatedCount;

    @Schema(description = "未变更模板数量")
    private int unchangedCount;

    @Schema(description = "跳过模板数量（用户自定义的不覆盖）")
    private int skippedCount;

    @Schema(description = "总处理数量")
    private int totalProcessed;

    @Schema(description = "耗时（毫秒）")
    private long durationMs;

    @Schema(description = "同步开始时间")
    private LocalDateTime startTime;

    @Schema(description = "同步结束时间")
    private LocalDateTime endTime;

    @Schema(description = "错误信息列表")
    private List<String> errors;
}
