package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.request.CreateModelApiKeyRequest;
import com.fsa.aicenter.application.dto.request.UpdateModelApiKeyRequest;
import com.fsa.aicenter.application.dto.response.ModelApiKeyResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.model.entity.ModelApiKey;
import com.fsa.aicenter.domain.model.repository.ModelApiKeyRepository;
import com.fsa.aicenter.domain.model.valueobject.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型API Key管理服务
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelApiKeyManagementService {

    private final ModelApiKeyRepository modelApiKeyRepository;

    /**
     * 为模型添加API Key
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "model:keys", key = "#request.modelId")
    public ModelApiKeyResponse create(CreateModelApiKeyRequest request) {
        if (request.getModelId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模型ID不能为空");
        }

        log.info("创建模型API Key, modelId: {}, keyName: {}", request.getModelId(), request.getKeyName());

        ModelApiKey modelApiKey = new ModelApiKey();
        BeanUtils.copyProperties(request, modelApiKey);

        // 默认值
        if (modelApiKey.getWeight() == null) {
            modelApiKey.setWeight(1);
        }
        if (modelApiKey.getHealthStatus() == null) {
            modelApiKey.setHealthStatus(HealthStatus.HEALTHY);
        }
        if (modelApiKey.getTotalRequests() == null) {
            modelApiKey.setTotalRequests(0L);
            modelApiKey.setSuccessRequests(0L);
            modelApiKey.setFailedRequests(0L);
        }
        if (modelApiKey.getFailCount() == null) {
            modelApiKey.setFailCount(0);
        }
        if (modelApiKey.getStatus() == null) {
            modelApiKey.setStatus(1);
        }

        // TODO: 对API Key进行加密存储
        // modelApiKey.setApiKey(encryptApiKey(request.getApiKey()));

        ModelApiKey saved = modelApiKeyRepository.save(modelApiKey);
        return toResponse(saved);
    }

    /**
     * 更新API Key
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "model:keys", allEntries = true)
    public void update(Long id, UpdateModelApiKeyRequest request) {
        log.info("更新模型API Key, id: {}", id);

        ModelApiKey modelApiKey = modelApiKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_KEY_NOT_FOUND));

        // 更新字段
        if (request.getKeyName() != null) {
            modelApiKey.setKeyName(request.getKeyName());
        }
        if (request.getApiKey() != null) {
            // TODO: 加密
            modelApiKey.setApiKey(request.getApiKey());
        }
        if (request.getWeight() != null) {
            modelApiKey.setWeight(request.getWeight());
        }
        if (request.getRateLimitPerMinute() != null) {
            modelApiKey.setRateLimitPerMinute(request.getRateLimitPerMinute());
        }
        if (request.getRateLimitPerDay() != null) {
            modelApiKey.setRateLimitPerDay(request.getRateLimitPerDay());
        }
        if (request.getQuotaTotal() != null) {
            modelApiKey.setQuotaTotal(request.getQuotaTotal());
        }
        if (request.getExpireTime() != null) {
            modelApiKey.setExpireTime(request.getExpireTime());
        }
        if (request.getSortOrder() != null) {
            modelApiKey.setSortOrder(request.getSortOrder());
        }
        if (request.getDescription() != null) {
            modelApiKey.setDescription(request.getDescription());
        }

        modelApiKeyRepository.update(modelApiKey);
    }

    /**
     * 删除API Key
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "model:keys", allEntries = true)
    public void delete(Long id) {
        log.info("删除模型API Key, id: {}", id);
        modelApiKeyRepository.deleteById(id);
    }

    /**
     * 启用/禁用API Key
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "model:keys", allEntries = true)
    public void toggleStatus(Long id) {
        ModelApiKey modelApiKey = modelApiKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_KEY_NOT_FOUND));

        if (modelApiKey.getStatus() == 1) {
            modelApiKey.disable();
        } else {
            modelApiKey.enable();
        }

        modelApiKeyRepository.update(modelApiKey);
    }

    /**
     * 重置健康状态
     * 将健康状态重置为健康，并清零失败计数
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "model:keys", allEntries = true)
    public void resetHealthStatus(Long id) {
        log.info("重置模型API Key健康状态, id: {}", id);

        ModelApiKey modelApiKey = modelApiKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_KEY_NOT_FOUND));

        modelApiKey.setHealthStatus(HealthStatus.HEALTHY);
        modelApiKey.setFailCount(0);

        modelApiKeyRepository.update(modelApiKey);
    }

    /**
     * 获取模型的所有Key
     */
    public List<ModelApiKeyResponse> listByModelId(Long modelId) {
        List<ModelApiKey> keys = modelApiKeyRepository.findByModelId(modelId);
        return keys.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取Key详情
     */
    public ModelApiKeyResponse getById(Long id) {
        ModelApiKey modelApiKey = modelApiKeyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MODEL_KEY_NOT_FOUND));
        return toResponse(modelApiKey);
    }

    /**
     * 转换为响应对象（脱敏）
     */
    private ModelApiKeyResponse toResponse(ModelApiKey modelApiKey) {
        ModelApiKeyResponse response = new ModelApiKeyResponse();
        BeanUtils.copyProperties(modelApiKey, response);

        // 设置完整的API Key（用于复制）
        response.setApiKey(modelApiKey.getApiKey());

        // 脱敏API Key：只显示前4位和后4位（用于显示）
        if (modelApiKey.getApiKey() != null && modelApiKey.getApiKey().length() > 8) {
            String masked = modelApiKey.getApiKey().substring(0, 4) + "****"
                    + modelApiKey.getApiKey().substring(modelApiKey.getApiKey().length() - 4);
            response.setApiKeyMasked(masked);
        } else {
            response.setApiKeyMasked("****");
        }

        return response;
    }
}
