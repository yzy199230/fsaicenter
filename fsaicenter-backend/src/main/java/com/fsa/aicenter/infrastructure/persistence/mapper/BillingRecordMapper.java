package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.BillingRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计费记录Mapper接口
 */
@Mapper
public interface BillingRecordMapper extends BaseMapper<BillingRecordPO> {

    /**
     * 根据请求ID查询计费记录
     *
     * @param requestId 请求ID
     * @return 计费记录
     */
    BillingRecordPO selectByRequestId(@Param("requestId") String requestId);

    /**
     * 根据API密钥ID和时间范围查询计费记录
     *
     * @param apiKeyId  API密钥ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 计费记录列表
     */
    List<BillingRecordPO> selectByApiKeyIdAndTimeRange(
        @Param("apiKeyId") Long apiKeyId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据模型ID和时间范围查询计费记录
     *
     * @param modelId   模型ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 计费记录列表
     */
    List<BillingRecordPO> selectByModelIdAndTimeRange(
        @Param("modelId") Long modelId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 批量插入计费记录
     *
     * @param list 计费记录列表
     */
    void insertBatch(@Param("list") List<BillingRecordPO> list);
}
