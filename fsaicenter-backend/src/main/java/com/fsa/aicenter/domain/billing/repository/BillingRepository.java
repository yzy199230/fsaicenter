package com.fsa.aicenter.domain.billing.repository;

import com.fsa.aicenter.domain.billing.aggregate.BillingRecord;
import com.fsa.aicenter.domain.billing.valueobject.BillingRule;
import com.fsa.aicenter.domain.billing.valueobject.BillingType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 计费记录仓储接口
 */
public interface BillingRepository {
    // ========== BillingRecord操作 ==========

    Optional<BillingRecord> findById(Long id);
    Optional<BillingRecord> findByRequestId(String requestId);

    /**
     * 按API密钥ID查询计费记录
     * 警告：可能返回大量数据，调用方应限制时间范围（建议不超过1个月）
     * TODO: 未来版本改为分页查询
     */
    List<BillingRecord> findByApiKeyId(Long apiKeyId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按模型ID查询计费记录
     * 警告：可能返回大量数据，调用方应限制时间范围（建议不超过1个月）
     * TODO: 未来版本改为分页查询
     */
    List<BillingRecord> findByModelId(Long modelId, LocalDateTime startTime, LocalDateTime endTime);

    void save(BillingRecord record);
    void batchSave(List<BillingRecord> records);

    // ========== BillingRule操作 ==========

    /**
     * 保存计费规则
     */
    void saveRule(BillingRule rule);

    /**
     * 更新计费规则
     */
    void updateRule(BillingRule rule);

    /**
     * 删除计费规则
     */
    void deleteRule(Long id);

    /**
     * 根据ID查询计费规则
     */
    Optional<BillingRule> findRuleById(Long id);

    /**
     * 查询有效的计费规则
     *
     * @param modelId     模型ID
     * @param type        计费类型
     * @param time        查询时间点
     * @return 有效的计费规则
     */
    Optional<BillingRule> findEffectiveRule(Long modelId, BillingType type, LocalDateTime time);

    /**
     * 根据模型ID查询所有计费规则
     *
     * @param modelId 模型ID
     * @return 计费规则列表
     */
    List<BillingRule> findRulesByModelId(Long modelId);
}
