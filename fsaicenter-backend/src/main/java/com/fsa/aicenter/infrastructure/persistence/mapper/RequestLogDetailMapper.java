package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.RequestLogDetailPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 请求日志详情Mapper接口
 */
@Mapper
public interface RequestLogDetailMapper extends BaseMapper<RequestLogDetailPO> {

    /**
     * 根据请求ID查询日志详情
     *
     * @param requestId 请求ID
     * @return 日志详情
     */
    RequestLogDetailPO selectByRequestId(@Param("requestId") String requestId);

    /**
     * 插入或更新日志详情（使用ON CONFLICT）
     *
     * @param po 日志详情PO
     * @return
     */
    boolean insertOrUpdate(RequestLogDetailPO po);
}
