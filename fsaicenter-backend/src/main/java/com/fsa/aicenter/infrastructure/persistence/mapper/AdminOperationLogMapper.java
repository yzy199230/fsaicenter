package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.po.AdminOperationLogPO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Mapper
 *
 * @author FSA AI Center
 */
@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLogPO> {

    @Select("SELECT * FROM admin_operation_log WHERE user_id = #{userId} AND is_deleted = 0 " +
            "ORDER BY created_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<AdminOperationLogPO> findByUserId(@Param("userId") Long userId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    @Select("SELECT * FROM admin_operation_log WHERE created_time >= #{startTime} AND created_time <= #{endTime} " +
            "AND is_deleted = 0 ORDER BY created_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<AdminOperationLogPO> findByTimeRange(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);
}
