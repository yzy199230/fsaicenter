package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.RequestLogPO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 请求日志Mapper接口
 */
@Mapper
public interface RequestLogMapper extends BaseMapper<RequestLogPO> {

    @Select("SELECT * FROM request_log WHERE request_id = #{requestId} AND is_deleted = 0")
    RequestLogPO selectByRequestId(@Param("requestId") String requestId);

    @Select("SELECT * FROM request_log WHERE api_key_id = #{apiKeyId} " +
            "AND created_time >= #{startTime} AND created_time <= #{endTime} AND is_deleted = 0 ORDER BY created_time DESC")
    List<RequestLogPO> selectByApiKeyIdAndTimeRange(
        @Param("apiKeyId") Long apiKeyId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("SELECT * FROM request_log WHERE model_id = #{modelId} " +
            "AND created_time >= #{startTime} AND created_time <= #{endTime} AND is_deleted = 0 ORDER BY created_time DESC")
    List<RequestLogPO> selectByModelIdAndTimeRange(
        @Param("modelId") Long modelId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("SELECT * FROM request_log WHERE status = #{status} " +
            "AND created_time >= #{startTime} AND created_time <= #{endTime} AND is_deleted = 0 ORDER BY created_time DESC")
    List<RequestLogPO> selectByStatusAndTimeRange(
        @Param("status") Integer status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Insert("<script>INSERT INTO request_log (request_id, api_key_id, model_id, request_type, is_stream, prompt_tokens, completion_tokens, total_tokens, request_ip, user_agent, http_status, response_time_ms, error_message, status, created_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.requestId}, #{item.apiKeyId}, #{item.modelId}, #{item.requestType}, #{item.isStream}, #{item.promptTokens}, #{item.completionTokens}, #{item.totalTokens}, #{item.requestIp}, #{item.userAgent}, #{item.httpStatus}, #{item.responseTimeMs}, #{item.errorMessage}, #{item.status}, #{item.createdTime})" +
            "</foreach></script>")
    void insertBatch(@Param("list") List<RequestLogPO> list);
}
