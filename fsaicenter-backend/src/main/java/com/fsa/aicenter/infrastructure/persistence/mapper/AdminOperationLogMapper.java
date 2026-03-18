package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminOperationLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Mapper
 *
 * @author FSA AI Center
 */
@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLogPO> {

    /**
     * 根据用户ID查询日志列表
     */
    List<AdminOperationLogPO> findByUserId(@Param("userId") Long userId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /**
     * 根据时间范围查询日志列表
     */
    List<AdminOperationLogPO> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);
}
