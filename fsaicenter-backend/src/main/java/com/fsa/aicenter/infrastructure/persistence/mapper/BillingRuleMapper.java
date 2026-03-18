package com.fsa.aicenter.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fsa.aicenter.infrastructure.persistence.entity.BillingRulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计费规则Mapper接口
 */
@Mapper
public interface BillingRuleMapper extends BaseMapper<BillingRulePO> {

    /**
     * 查询有效的计费规则
     *
     * @param modelId     模型ID
     * @param billingType 计费类型
     * @param time        查询时间点
     * @return 有效的计费规则（最新的一条）
     */
    BillingRulePO selectEffectiveRule(
        @Param("modelId") Long modelId,
        @Param("billingType") String billingType,
        @Param("time") LocalDateTime time
    );

    /**
     * 根据模型ID查询所有计费规则
     *
     * @param modelId 模型ID
     * @return 计费规则列表
     */
    List<BillingRulePO> selectByModelId(@Param("modelId") Long modelId);
}
