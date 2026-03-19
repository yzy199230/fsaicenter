package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.BillingRulePO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计费规则Mapper接口
 */
@Mapper
public interface BillingRuleMapper extends BaseMapper<BillingRulePO> {

    @Select("SELECT * FROM billing_rule WHERE model_id = #{modelId} AND billing_type = #{billingType} " +
            "AND effective_time <= #{time} AND (expire_time IS NULL OR expire_time > #{time}) " +
            "AND status = 1 AND is_deleted = 0 ORDER BY effective_time DESC LIMIT 1")
    BillingRulePO selectEffectiveRule(
        @Param("modelId") Long modelId,
        @Param("billingType") String billingType,
        @Param("time") LocalDateTime time
    );

    @Select("SELECT * FROM billing_rule WHERE model_id = #{modelId} AND is_deleted = 0 ORDER BY effective_time DESC")
    List<BillingRulePO> selectByModelId(@Param("modelId") Long modelId);
}
