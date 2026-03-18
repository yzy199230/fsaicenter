package com.fsa.aicenter.infrastructure.persistence.repository;

import com.fsa.aicenter.domain.billing.aggregate.BillingRecord;
import com.fsa.aicenter.domain.billing.repository.BillingRepository;
import com.fsa.aicenter.domain.billing.valueobject.BillingRule;
import com.fsa.aicenter.domain.billing.valueobject.BillingType;
import com.fsa.aicenter.domain.billing.valueobject.CostAmount;
import com.fsa.aicenter.infrastructure.exception.RepositoryException;
import com.fsa.aicenter.infrastructure.persistence.entity.BillingRecordPO;
import com.fsa.aicenter.infrastructure.persistence.entity.BillingRulePO;
import com.fsa.aicenter.infrastructure.persistence.mapper.BillingRecordMapper;
import com.fsa.aicenter.infrastructure.persistence.mapper.BillingRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 计费仓储实现类
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BillingRepositoryImpl implements BillingRepository {

    private static final int COST_SCALE = 4;   // 总费用小数位数
    private static final Integer STATUS_ENABLED = 1;  // 启用状态
    private static final Integer STATUS_DISABLED = 0;  // 禁用状态

    private final BillingRecordMapper billingRecordMapper;
    private final BillingRuleMapper billingRuleMapper;

    // ========== BillingRecord操作 ==========

    @Override
    public Optional<BillingRecord> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Billing record ID cannot be null");
        }

        BillingRecordPO po = billingRecordMapper.selectById(id);
        return Optional.ofNullable(po).map(this::recordToDomain);
    }

    @Override
    public Optional<BillingRecord> findByRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        BillingRecordPO po = billingRecordMapper.selectByRequestId(requestId);
        return Optional.ofNullable(po).map(this::recordToDomain);
    }

    @Override
    public List<BillingRecord> findByApiKeyId(Long apiKeyId, LocalDateTime startTime, LocalDateTime endTime) {
        if (apiKeyId == null) {
            throw new IllegalArgumentException("API key ID cannot be null");
        }
        validateTimeRange(startTime, endTime);

        List<BillingRecordPO> poList = billingRecordMapper.selectByApiKeyIdAndTimeRange(
            apiKeyId, startTime, endTime
        );

        return poList.stream()
            .map(this::recordToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<BillingRecord> findByModelId(Long modelId, LocalDateTime startTime, LocalDateTime endTime) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }
        validateTimeRange(startTime, endTime);

        List<BillingRecordPO> poList = billingRecordMapper.selectByModelIdAndTimeRange(
            modelId, startTime, endTime
        );

        return poList.stream()
            .map(this::recordToDomain)
            .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void save(BillingRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Billing record cannot be null");
        }

        log.info("Saving billing record: requestId={}, apiKeyId={}, modelId={}, cost={}",
            record.getRequestId(), record.getApiKeyId(), record.getModelId(),
            record.getTotalCost());

        BillingRecordPO po = recordToPO(record);
        billingRecordMapper.insert(po);
        record.setId(po.getId());

        log.debug("Billing record saved with id={}", po.getId());
    }

    /**
     * 批量保存计费记录
     *
     * @param records 计费记录列表
     * @throws IllegalArgumentException 如果records为null
     * @apiNote 如果records为空列表，方法将静默返回，不执行任何操作
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchSave(List<BillingRecord> records) {
        if (records == null) {
            throw new IllegalArgumentException("Records cannot be null");
        }
        if (records.isEmpty()) {
            log.debug("Batch save called with empty list, skipping");
            return;
        }

        log.info("Batch saving {} billing records", records.size());

        // 转换为PO列表
        List<BillingRecordPO> poList = records.stream()
            .map(this::recordToPO)
            .collect(Collectors.toList());

        // 使用MyBatis的批量插入（分批处理，每批1000条）
        int batchSize = 1000;
        for (int i = 0; i < poList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, poList.size());
            List<BillingRecordPO> batch = poList.subList(i, end);

            // 批量插入
            billingRecordMapper.insertBatch(batch);

            log.debug("Batch inserted {} records (batch {}/{})",
                batch.size(), (i / batchSize) + 1, (poList.size() + batchSize - 1) / batchSize);
        }

        // 回设ID到原对象
        for (int i = 0; i < records.size(); i++) {
            records.get(i).setId(poList.get(i).getId());
        }

        log.info("Batch saved {} billing records", records.size());
    }

    // ========== BillingRule操作 ==========

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveRule(BillingRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Billing rule cannot be null");
        }

        log.info("Saving billing rule: modelId={}, billingType={}, unitPrice={}",
            rule.getModelId(), rule.getBillingType(), rule.getUnitPrice());

        BillingRulePO po = ruleToPO(rule);
        billingRuleMapper.insert(po);

        log.debug("Billing rule saved with id={}", po.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateRule(BillingRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Billing rule cannot be null");
        }
        if (rule.getId() == null) {
            throw new IllegalArgumentException("Billing rule ID cannot be null for update");
        }

        log.info("Updating billing rule: id={}, modelId={}, billingType={}",
            rule.getId(), rule.getModelId(), rule.getBillingType());

        BillingRulePO po = ruleToPO(rule);
        int rows = billingRuleMapper.updateById(po);

        if (rows == 0) {
            log.error("Billing rule not found for update: id={}", rule.getId());
            throw new RepositoryException("Billing rule not found: " + rule.getId());
        }

        log.info("Billing rule updated: id={}", rule.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteRule(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Billing rule ID cannot be null");
        }

        log.info("Deleting billing rule: id={}", id);

        int rows = billingRuleMapper.deleteById(id);

        if (rows == 0) {
            log.warn("Billing rule not found for delete: id={}", id);
        } else {
            log.info("Billing rule deleted: id={}", id);
        }
    }

    @Override
    public Optional<BillingRule> findRuleById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Billing rule ID cannot be null");
        }

        BillingRulePO po = billingRuleMapper.selectById(id);
        return Optional.ofNullable(po).map(this::ruleToDomain);
    }

    @Override
    public Optional<BillingRule> findEffectiveRule(Long modelId, BillingType type, LocalDateTime time) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Billing type cannot be null");
        }
        if (time == null) {
            time = LocalDateTime.now();
        }

        BillingRulePO po = billingRuleMapper.selectEffectiveRule(modelId, type.getCode(), time);
        return Optional.ofNullable(po).map(this::ruleToDomain);
    }

    @Override
    public List<BillingRule> findRulesByModelId(Long modelId) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }

        List<BillingRulePO> poList = billingRuleMapper.selectByModelId(modelId);

        return poList.stream()
            .map(this::ruleToDomain)
            .collect(Collectors.toList());
    }

    // ========== 辅助方法 ==========

    /**
     * 校验时间范围参数
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    // ========== 转换方法 ==========

    /**
     * BillingRecord: PO → 领域对象
     */
    private BillingRecord recordToDomain(BillingRecordPO po) {
        try {
            BillingRecord record = new BillingRecord();
            record.setId(po.getId());
            record.setRequestId(po.getRequestId());
            record.setApiKeyId(po.getApiKeyId());
            record.setModelId(po.getModelId());
            record.setBillingType(BillingType.fromCode(po.getBillingType()));
            record.setUsageAmount(po.getUsageAmount());
            record.setUnitPrice(po.getUnitPrice());
            record.setTotalCost(new CostAmount(po.getTotalCost(), po.getCurrency()));
            record.setBillingTime(po.getBillingTime());
            record.setCreatedTime(po.getCreatedTime());

            return record;
        } catch (Exception e) {
            log.error("Failed to convert BillingRecordPO to domain: id={}", po.getId(), e);
            throw new RepositoryException("Failed to convert billing record: " + po.getId(), e);
        }
    }

    /**
     * BillingRecord: 领域对象 → PO
     */
    private BillingRecordPO recordToPO(BillingRecord record) {
        BillingRecordPO po = new BillingRecordPO();
        po.setId(record.getId());
        po.setRequestId(record.getRequestId());
        po.setApiKeyId(record.getApiKeyId());
        po.setModelId(record.getModelId());
        po.setBillingType(record.getBillingType().getCode());
        po.setUsageAmount(record.getUsageAmount());
        po.setUnitPrice(record.getUnitPrice());  // BillingRule构造器中已设置精度，无需重复
        po.setTotalCost(record.getTotalCost().getAmount().setScale(COST_SCALE, RoundingMode.HALF_UP));
        po.setCurrency(record.getTotalCost().getCurrency());
        po.setBillingTime(record.getBillingTime());
        po.setCreatedTime(record.getCreatedTime());

        return po;
    }

    /**
     * BillingRule: PO → 领域对象
     */
    private BillingRule ruleToDomain(BillingRulePO po) {
        try {
            return new BillingRule(
                po.getId(),
                po.getModelId(),
                BillingType.fromCode(po.getBillingType()),
                po.getUnitPrice(),
                po.getInputUnitPrice(),
                po.getOutputUnitPrice(),
                po.getUnitAmount(),
                po.getCurrency(),
                po.getEffectiveTime(),
                po.getExpireTime(),
                po.getDescription()
            );
        } catch (Exception e) {
            log.error("Failed to convert BillingRulePO to domain: id={}", po.getId(), e);
            throw new RepositoryException("Failed to convert billing rule: " + po.getId(), e);
        }
    }

    /**
     * BillingRule: 领域对象 → PO
     */
    private BillingRulePO ruleToPO(BillingRule rule) {
        BillingRulePO po = new BillingRulePO();
        po.setId(rule.getId());
        po.setModelId(rule.getModelId());
        po.setBillingType(rule.getBillingType().getCode());
        po.setUnitPrice(rule.getUnitPrice());
        po.setInputUnitPrice(rule.getInputUnitPrice());
        po.setOutputUnitPrice(rule.getOutputUnitPrice());
        po.setUnitAmount(rule.getUnitAmount());
        po.setCurrency(rule.getCurrency());
        po.setEffectiveTime(rule.getEffectiveTime());
        po.setExpireTime(rule.getExpireTime());
        po.setDescription(rule.getDescription());
        po.setStatus(STATUS_ENABLED);  // 新规则默认启用

        return po;
    }
}
