package com.fsa.aicenter.domain.admin.repository;

import com.fsa.aicenter.domain.admin.aggregate.AdminOperationLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 操作日志仓储接口
 *
 * @author FSA AI Center
 */
public interface AdminOperationLogRepository {

    /**
     * 保存操作日志
     */
    AdminOperationLog save(AdminOperationLog log);

    /**
     * 根据ID查询
     */
    Optional<AdminOperationLog> findById(Long id);

    /**
     * 根据用户ID查询日志列表
     */
    List<AdminOperationLog> findByUserId(Long userId, int offset, int limit);

    /**
     * 根据时间范围查询日志列表
     */
    List<AdminOperationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, int offset, int limit);

    /**
     * 统计总数
     */
    long count();
}
