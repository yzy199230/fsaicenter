package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.RequestLogDetailPO;
import org.apache.ibatis.annotations.*;

/**
 * 请求日志详情Mapper接口
 */
@Mapper
public interface RequestLogDetailMapper extends BaseMapper<RequestLogDetailPO> {

    @Select("SELECT * FROM request_log_detail WHERE request_id = #{requestId} AND is_deleted = 0")
    RequestLogDetailPO selectByRequestId(@Param("requestId") String requestId);

    @Insert("INSERT INTO request_log_detail (request_id, request_body, response_body) " +
            "VALUES (#{requestId}, #{requestBody}, #{responseBody}) " +
            "ON CONFLICT (request_id) DO UPDATE SET request_body = #{requestBody}, response_body = #{responseBody}")
    boolean insertOrUpdate(RequestLogDetailPO po);
}
