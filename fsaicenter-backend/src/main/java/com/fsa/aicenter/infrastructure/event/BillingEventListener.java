package com.fsa.aicenter.infrastructure.event;

import com.fsa.aicenter.application.event.BillingEvent;
import com.fsa.aicenter.domain.apikey.repository.ApiKeyRepository;
import com.fsa.aicenter.domain.billing.aggregate.BillingRecord;
import com.fsa.aicenter.domain.billing.repository.BillingRepository;
import com.fsa.aicenter.domain.billing.valueobject.BillingRule;
import com.fsa.aicenter.domain.billing.valueobject.BillingType;
import com.fsa.aicenter.domain.billing.valueobject.UsageMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 计费事件监听器
 * <p>
 * 使用Virtual Thread异步处理计费事件，不阻塞主流程。
 * 主要职责：
 * 1. 从事件中提取计费信息
 * 2. 查询计费规则
 * 3. 创建计费记录
 * 4. 更新ApiKey使用统计
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class BillingEventListener {

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    /**
     * 处理计费事件
     * <p>
     * 使用@Async启用异步处理（Virtual Thread），@Transactional确保事务一致性。
     * 异常不会影响主流程，但会被记录并可能重试。
     * </p>
     *
     * @param event 计费事件
     */
    @Async
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleBillingEvent(BillingEvent event) {
        if (event == null) {
            log.warn("收到空计费事件，跳过处理");
            return;
        }

        log.info("开始处理计费事件: requestId={}, apiKeyId={}, modelId={}, tokens={}, cost={}",
                event.getRequestId(), event.getApiKey().getId(), event.getModel().getId(),
                event.getTokens(), event.getCost());

        try {
            // 1. 确定计费类型和使用量
            BillingType billingType;
            long usageAmount;

            if (event.isTokenBased()) {
                billingType = BillingType.TOKEN;
                usageAmount = event.getTokens();
            } else {
                // 根据模型类型判断计费方式
                if (event.getCost() != null) {
                    billingType = BillingType.IMAGE;
                    usageAmount = event.getCost();
                } else {
                    log.warn("计费事件既无tokens也无cost，使用默认值0: requestId={}", event.getRequestId());
                    billingType = BillingType.TOKEN;
                    usageAmount = 0;
                }
            }

            // 2. 查询有效的计费规则
            Optional<BillingRule> ruleOpt = billingRepository.findEffectiveRule(
                    event.getModel().getId(),
                    billingType,
                    event.getRequestTime()
            );

            if (ruleOpt.isEmpty()) {
                log.warn("未找到有效计费规则: modelId={}, billingType={}, time={}",
                        event.getModel().getId(), billingType, event.getRequestTime());
                // 创建零成本计费记录（用于统计）
                createZeroCostRecord(event, billingType, usageAmount);
                return;
            }

            BillingRule rule = ruleOpt.get();

            // 3. 创建使用指标
            UsageMetrics metrics = new UsageMetrics(billingType, usageAmount);

            // 4. 创建计费记录（自动计算成本）
            BillingRecord record = BillingRecord.create(
                    event.getRequestId(),
                    event.getApiKey().getId(),
                    rule,
                    metrics
            );

            // 5. 保存计费记录
            billingRepository.save(record);

            // 6. 更新ApiKey统计信息（可选，根据业务需求）
            updateApiKeyStatistics(event, record);

            // 7. 计算处理耗时
            long processingTime = Duration.between(event.getEventTime(), LocalDateTime.now()).toMillis();

            log.info("计费事件处理成功: requestId={}, billingRecordId={}, totalCost={}, processingTime={}ms",
                    event.getRequestId(), record.getId(), record.getTotalCost(), processingTime);

        } catch (Exception e) {
            log.error("计费事件处理失败: requestId={}", event.getRequestId(), e);
            // 异常会触发事务回滚
            // 根据业务需求，可以考虑：
            // 1. 重试机制（使用@Retryable）
            // 2. 死信队列
            // 3. 告警通知
            throw e; // 重新抛出异常以确保事务回滚
        }
    }

    /**
     * 创建零成本计费记录
     * <p>
     * 当找不到计费规则时，创建零成本记录用于统计。
     * </p>
     */
    private void createZeroCostRecord(BillingEvent event, BillingType billingType, long usageAmount) {
        try {
            BillingRecord record = new BillingRecord();
            record.setRequestId(event.getRequestId());
            record.setApiKeyId(event.getApiKey().getId());
            record.setModelId(event.getModel().getId());
            record.setBillingType(billingType);
            record.setUsageAmount(usageAmount);
            record.setUnitPrice(java.math.BigDecimal.ZERO);
            record.setTotalCost(new com.fsa.aicenter.domain.billing.valueobject.CostAmount(
                    java.math.BigDecimal.ZERO, "CNY"));
            record.setBillingTime(LocalDateTime.now());
            record.setCreatedTime(LocalDateTime.now());

            billingRepository.save(record);

            log.info("已创建零成本计费记录: requestId={}", event.getRequestId());
        } catch (Exception e) {
            log.error("创建零成本计费记录失败: requestId={}", event.getRequestId(), e);
        }
    }

    /**
     * 更新ApiKey使用统计
     * <p>
     * 可选功能，根据业务需求决定是否需要实时统计。
     * 如果需要高性能，建议使用定时任务批量统计。
     * </p>
     */
    private void updateApiKeyStatistics(BillingEvent event, BillingRecord record) {
        try {
            // TODO: 根据业务需求实现ApiKey统计更新
            // 例如：总调用次数、总成本、最后调用时间等
            // apiKeyRepository.incrementUsageCount(event.getApiKey().getId());
            // apiKeyRepository.addTotalCost(event.getApiKey().getId(), record.getTotalCost());

            log.debug("ApiKey统计信息已更新: apiKeyId={}", event.getApiKey().getId());
        } catch (Exception e) {
            // 统计更新失败不应影响主流程
            log.warn("更新ApiKey统计信息失败: apiKeyId={}", event.getApiKey().getId(), e);
        }
    }
}
