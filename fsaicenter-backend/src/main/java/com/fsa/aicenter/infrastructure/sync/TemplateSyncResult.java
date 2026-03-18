package com.fsa.aicenter.infrastructure.sync;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板同步结果
 *
 * @author FSA AI Center
 */
@Data
public class TemplateSyncResult {

    /** 数据源名称 */
    private String sourceName;

    /** 同步开始时间 */
    private LocalDateTime startTime;

    /** 同步结束时间 */
    private LocalDateTime endTime;

    /** 新增模板数量 */
    private int addedCount;

    /** 更新模板数量 */
    private int updatedCount;

    /** 未变更模板数量 */
    private int unchangedCount;

    /** 跳过模板数量（用户自定义的不覆盖） */
    private int skippedCount;

    /** 错误列表 */
    private List<String> errors = new ArrayList<>();

    /** 是否成功 */
    private boolean success;

    /** 总耗时（毫秒） */
    public long getDurationMs() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }

    /** 总处理数量 */
    public int getTotalProcessed() {
        return addedCount + updatedCount + unchangedCount + skippedCount;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public static TemplateSyncResult success(String sourceName) {
        TemplateSyncResult result = new TemplateSyncResult();
        result.setSourceName(sourceName);
        result.setSuccess(true);
        return result;
    }

    public static TemplateSyncResult failure(String sourceName, String error) {
        TemplateSyncResult result = new TemplateSyncResult();
        result.setSourceName(sourceName);
        result.setSuccess(false);
        result.addError(error);
        return result;
    }
}
