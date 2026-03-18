package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.request.BillingRuleQueryRequest;
import com.fsa.aicenter.application.dto.request.CreateBillingRuleRequest;
import com.fsa.aicenter.application.dto.request.UpdateBillingRuleRequest;
import com.fsa.aicenter.application.dto.response.BillingRuleResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.common.model.PageResult;
import com.fsa.aicenter.domain.billing.repository.BillingRepository;
import com.fsa.aicenter.domain.billing.valueobject.BillingRule;
import com.fsa.aicenter.domain.billing.valueobject.BillingType;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.repository.ModelRepository;
import com.fsa.aicenter.infrastructure.persistence.entity.BillingRulePO;
import com.fsa.aicenter.infrastructure.persistence.mapper.BillingRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 计费规则管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRuleManagementService {

    private final BillingRepository billingRepository;
    private final BillingRuleMapper billingRuleMapper;
    private final ModelRepository modelRepository;

    /**
     * 分页查询计费规则
     */
    public PageResult<BillingRuleResponse> listRules(BillingRuleQueryRequest request) {
        Page<BillingRulePO> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BillingRulePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BillingRulePO::getIsDeleted, 0);

        if (request.getModelId() != null) {
            wrapper.eq(BillingRulePO::getModelId, request.getModelId());
        }
        if (StringUtils.hasText(request.getBillingType())) {
            wrapper.eq(BillingRulePO::getBillingType, request.getBillingType());
        }
        if (request.getStatus() != null) {
            wrapper.eq(BillingRulePO::getStatus, request.getStatus());
        }

        wrapper.orderByDesc(BillingRulePO::getCreatedTime);

        Page<BillingRulePO> resultPage = billingRuleMapper.selectPage(page, wrapper);

        // 获取模型信息
        List<Long> modelIds = resultPage.getRecords().stream()
            .map(BillingRulePO::getModelId)
            .distinct()
            .collect(Collectors.toList());

        Map<Long, AiModel> modelMap = modelIds.stream()
            .map(id -> modelRepository.findById(id).orElse(null))
            .filter(m -> m != null)
            .collect(Collectors.toMap(AiModel::getId, m -> m));

        List<BillingRuleResponse> list = resultPage.getRecords().stream()
            .map(po -> toResponse(po, modelMap.get(po.getModelId())))
            .collect(Collectors.toList());

        return PageResult.of(resultPage.getTotal(), request.getPageNum(), request.getPageSize(), list);
    }

    /**
     * 查询计费规则详情
     */
    public BillingRuleResponse getRule(Long id) {
        BillingRulePO po = billingRuleMapper.selectById(id);
        if (po == null || po.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.BILLING_RULE_NOT_FOUND);
        }
        AiModel model = modelRepository.findById(po.getModelId()).orElse(null);
        return toResponse(po, model);
    }

    /**
     * 创建计费规则
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(CreateBillingRuleRequest request) {
        // 验证模型存在
        modelRepository.findById(request.getModelId())
            .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_NOT_FOUND));

        BillingType billingType = BillingType.fromCode(request.getBillingType());

        BillingRule rule = new BillingRule(
            null,
            request.getModelId(),
            billingType,
            request.getUnitPrice(),
            request.getInputUnitPrice(),
            request.getOutputUnitPrice(),
            request.getUnitAmount(),
            request.getCurrency(),
            request.getEffectiveTime(),
            request.getExpireTime(),
            request.getDescription()
        );

        billingRepository.saveRule(rule);

        log.info("创建计费规则成功: modelId={}, billingType={}", request.getModelId(), request.getBillingType());

        // 返回创建的规则ID（需要从数据库查询）
        List<BillingRule> rules = billingRepository.findRulesByModelId(request.getModelId());
        return rules.stream()
            .filter(r -> r.getBillingType() == billingType)
            .map(BillingRule::getId)
            .findFirst()
            .orElse(null);
    }

    /**
     * 更新计费规则
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(Long id, UpdateBillingRuleRequest request) {
        BillingRulePO existingPO = billingRuleMapper.selectById(id);
        if (existingPO == null || existingPO.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.BILLING_RULE_NOT_FOUND);
        }

        // 更新字段
        if (request.getUnitPrice() != null) {
            existingPO.setUnitPrice(request.getUnitPrice());
        }
        if (request.getInputUnitPrice() != null) {
            existingPO.setInputUnitPrice(request.getInputUnitPrice());
        }
        if (request.getOutputUnitPrice() != null) {
            existingPO.setOutputUnitPrice(request.getOutputUnitPrice());
        }
        if (request.getUnitAmount() != null) {
            existingPO.setUnitAmount(request.getUnitAmount());
        }
        if (request.getEffectiveTime() != null) {
            existingPO.setEffectiveTime(request.getEffectiveTime());
        }
        if (request.getExpireTime() != null) {
            existingPO.setExpireTime(request.getExpireTime());
        }
        if (request.getDescription() != null) {
            existingPO.setDescription(request.getDescription());
        }

        existingPO.setUpdatedTime(LocalDateTime.now());
        billingRuleMapper.updateById(existingPO);

        log.info("更新计费规则成功: id={}", id);
    }

    /**
     * 删除计费规则
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        BillingRulePO existingPO = billingRuleMapper.selectById(id);
        if (existingPO == null || existingPO.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.BILLING_RULE_NOT_FOUND);
        }

        billingRepository.deleteRule(id);
        log.info("删除计费规则成功: id={}", id);
    }

    /**
     * PO转响应DTO
     */
    private BillingRuleResponse toResponse(BillingRulePO po, AiModel model) {
        BillingRuleResponse response = new BillingRuleResponse();
        response.setId(po.getId());
        response.setModelId(po.getModelId());
        response.setBillingType(po.getBillingType());
        response.setUnitPrice(po.getUnitPrice());
        response.setInputUnitPrice(po.getInputUnitPrice());
        response.setOutputUnitPrice(po.getOutputUnitPrice());
        response.setUnitAmount(po.getUnitAmount());
        response.setCurrency(po.getCurrency());
        response.setEffectiveTime(po.getEffectiveTime());
        response.setExpireTime(po.getExpireTime());
        response.setDescription(po.getDescription());
        response.setStatus(po.getStatus());
        response.setCreatedTime(po.getCreatedTime());
        response.setUpdatedTime(po.getUpdatedTime());

        if (model != null) {
            response.setModelName(model.getName());
            response.setModelType(model.getType().getCode());
        }

        return response;
    }
}
