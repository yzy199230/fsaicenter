package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.BillingRecordPO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计费记录Mapper接口
 */
@Mapper
public interface BillingRecordMapper extends BaseMapper<BillingRecordPO> {

    @Select("SELECT * FROM billing_record WHERE request_id = #{requestId} AND is_deleted = 0")
    BillingRecordPO selectByRequestId(@Param("requestId") String requestId);

    @Select("SELECT * FROM billing_record WHERE api_key_id = #{apiKeyId} " +
            "AND billing_time >= #{startTime} AND billing_time <= #{endTime} AND is_deleted = 0 ORDER BY billing_time DESC")
    List<BillingRecordPO> selectByApiKeyIdAndTimeRange(
        @Param("apiKeyId") Long apiKeyId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Select("SELECT * FROM billing_record WHERE model_id = #{modelId} " +
            "AND billing_time >= #{startTime} AND billing_time <= #{endTime} AND is_deleted = 0 ORDER BY billing_time DESC")
    List<BillingRecordPO> selectByModelIdAndTimeRange(
        @Param("modelId") Long modelId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Insert("<script>INSERT INTO billing_record (request_id, api_key_id, model_id, billing_type, usage_amount, unit_price, total_cost, currency) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.requestId}, #{item.apiKeyId}, #{item.modelId}, #{item.billingType}, #{item.usageAmount}, #{item.unitPrice}, #{item.totalCost}, #{item.currency})" +
            "</foreach></script>")
    void insertBatch(@Param("list") List<BillingRecordPO> list);
}
