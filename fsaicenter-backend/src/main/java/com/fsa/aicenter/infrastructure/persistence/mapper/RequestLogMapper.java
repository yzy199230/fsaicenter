package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.RequestLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 请求日志Mapper接口
 */
@Mapper
public interface RequestLogMapper extends BaseMapper<RequestLogPO> {

    /**
     * 根据请求ID查询日志
     *
     * @param requestId 请求ID
     * @return 请求日志
     */
    RequestLogPO selectByRequestId(@Param("requestId") String requestId);

    /**
     * 根据API密钥ID和时间范围查询日志
     *
     * @param apiKeyId  API密钥ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 请求日志列表
     */
    List<RequestLogPO> selectByApiKeyIdAndTimeRange(
        @Param("apiKeyId") Long apiKeyId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据模型ID和时间范围查询日志
     *
     * @param modelId   模型ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 请求日志列表
     */
    List<RequestLogPO> selectByModelIdAndTimeRange(
        @Param("modelId") Long modelId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据状态和时间范围查询日志
     *
     * @param status    状态（1=成功，0=失败）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 请求日志列表
     */
    List<RequestLogPO> selectByStatusAndTimeRange(
        @Param("status") Integer status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 批量插入日志记录
     *
     * @param list 日志记录列表
     */
    void insertBatch(@Param("list") List<RequestLogPO> list);
}
