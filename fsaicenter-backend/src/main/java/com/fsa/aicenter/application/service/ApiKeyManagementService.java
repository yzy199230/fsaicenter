package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.request.CreateApiKeyRequest;
import com.fsa.aicenter.application.dto.request.UpdateApiKeyRequest;
import com.fsa.aicenter.application.dto.response.ApiKeyResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.apikey.repository.ApiKeyRepository;
import com.fsa.aicenter.domain.apikey.valueobject.AccessControl;
import com.fsa.aicenter.domain.apikey.valueobject.Quota;
import com.fsa.aicenter.domain.apikey.valueobject.RateLimit;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API密钥管理服务
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyManagementService {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * 创建API密钥
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createApiKey(CreateApiKeyRequest request) {
        // 生成API Key
        String keyValue = generateApiKey();

        // 构建领域对象
        ApiKey apiKey = new ApiKey();
        apiKey.setKeyValue(keyValue);
        apiKey.setKeyName(request.getKeyName());
        apiKey.setDescription(request.getDescription());

        // 设置配额
        Quota quota = new Quota(request.getQuotaTotal(), 0L);
        apiKey.setQuota(quota);

        // 设置限流
        RateLimit rateLimit = new RateLimit(
            request.getRateLimitPerMinute(),
            request.getRateLimitPerDay()
        );
        apiKey.setRateLimit(rateLimit);

        // 设置访问控制
        AccessControl accessControl = AccessControl.of(
            request.getAllowedModelTypes(),
            request.getAllowedIpWhitelist()
        );
        apiKey.setAccessControl(accessControl);

        apiKey.setExpireTime(request.getExpireTime());
        apiKey.setStatus(EntityStatus.ENABLED);

        // 保存
        ApiKey savedApiKey = apiKeyRepository.save(apiKey);

        // 同步模型访问权限
        if (request.getAllowedModelIds() != null && !request.getAllowedModelIds().isEmpty()) {
            apiKeyRepository.syncModelAccess(savedApiKey.getId(), request.getAllowedModelIds());
        }

        log.info("创建API密钥成功: id={}, keyName={}", savedApiKey.getId(), savedApiKey.getKeyName());

        return savedApiKey.getId();
    }

    /**
     * 更新API密钥
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateApiKey(Long id, UpdateApiKeyRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_NOT_FOUND));

        // 更新基本信息
        apiKey.setKeyName(request.getKeyName());
        apiKey.setDescription(request.getDescription());

        // 更新配额（保留已使用量）
        if (request.getQuotaTotal() != null) {
            Long usedQuota = apiKey.getQuota() != null ? apiKey.getQuota().getUsed() : 0L;
            Quota newQuota = new Quota(request.getQuotaTotal(), usedQuota);
            apiKey.setQuota(newQuota);
        }

        // 更新限流
        if (request.getRateLimitPerMinute() != null || request.getRateLimitPerDay() != null) {
            RateLimit newRateLimit = new RateLimit(
                request.getRateLimitPerMinute(),
                request.getRateLimitPerDay()
            );
            apiKey.setRateLimit(newRateLimit);
        }

        // 更新访问控制
        if (request.getAllowedModelTypes() != null || request.getAllowedIpWhitelist() != null) {
            AccessControl newAccessControl = AccessControl.of(
                request.getAllowedModelTypes(),
                request.getAllowedIpWhitelist()
            );
            apiKey.setAccessControl(newAccessControl);
        }

        // 更新过期时间
        if (request.getExpireTime() != null) {
            apiKey.setExpireTime(request.getExpireTime());
        }

        apiKeyRepository.update(apiKey);

        // 同步模型访问权限（null表示不更新）
        if (request.getAllowedModelIds() != null) {
            apiKeyRepository.syncModelAccess(id, request.getAllowedModelIds());
        }

        log.info("更新API密钥成功: id={}, keyName={}", apiKey.getId(), apiKey.getKeyName());
    }

    /**
     * 删除API密钥（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_NOT_FOUND));

        apiKeyRepository.delete(id);
        log.info("删除API密钥成功: id={}, keyName={}", apiKey.getId(), apiKey.getKeyName());
    }

    /**
     * 启用/禁用API密钥
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleApiKeyStatus(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_NOT_FOUND));

        EntityStatus newStatus = apiKey.isEnabled() ? EntityStatus.DISABLED : EntityStatus.ENABLED;
        apiKey.setStatus(newStatus);
        apiKeyRepository.update(apiKey);

        log.info("切换API密钥状态: id={}, status={}", apiKey.getId(), newStatus);
    }

    /**
     * 更新API密钥状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateApiKeyStatus(Long id, EntityStatus status) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_NOT_FOUND));

        apiKey.setStatus(status);
        apiKeyRepository.update(apiKey);

        log.info("更新API密钥状态: id={}, status={}", apiKey.getId(), status);
    }

    /**
     * 查询API密钥详情
     */
    public ApiKeyResponse getApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_NOT_FOUND));
        return toResponse(apiKey);
    }

    /**
     * 查询所有API密钥
     */
    public List<ApiKeyResponse> listApiKeys() {
        List<ApiKey> apiKeys = apiKeyRepository.findAll();
        return apiKeys.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 重置配额
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetQuota(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_NOT_FOUND));

        Long total = apiKey.getQuota() != null ? apiKey.getQuota().getTotal() : 0L;
        Quota newQuota = new Quota(total, 0L);
        apiKey.setQuota(newQuota);

        apiKeyRepository.update(apiKey);
        log.info("重置API密钥配额成功: id={}, keyName={}", apiKey.getId(), apiKey.getKeyName());
    }

    /**
     * 生成API Key
     * 格式: fsa_开头 + UUID去掉横线
     */
    private String generateApiKey() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "fsa_" + uuid;
    }

    /**
     * 领域对象转响应DTO
     */
    private ApiKeyResponse toResponse(ApiKey apiKey) {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(apiKey.getId());
        response.setKeyValue(apiKey.getKeyValue());
        response.setKeyName(apiKey.getKeyName());
        response.setDescription(apiKey.getDescription());

        // 配额信息
        if (apiKey.getQuota() != null) {
            response.setQuotaTotal(apiKey.getQuota().getTotal());
            response.setQuotaUsed(apiKey.getQuota().getUsed());
            response.setQuotaRemaining(apiKey.getQuota().remaining());
        }

        // 限流信息
        if (apiKey.getRateLimit() != null) {
            response.setRateLimitPerMinute(apiKey.getRateLimit().getPerMinute());
            response.setRateLimitPerDay(apiKey.getRateLimit().getPerDay());
        }

        // 访问控制
        if (apiKey.getAccessControl() != null) {
            response.setAllowedModelTypes(apiKey.getAccessControl().getAllowedModelTypes());
            response.setAllowedIpWhitelist(apiKey.getAccessControl().getAllowedIpWhitelist());
        }

        // 模型访问权限
        response.setAllowedModelIds(apiKeyRepository.findAccessibleModelIds(apiKey.getId()));

        response.setExpireTime(apiKey.getExpireTime());
        response.setStatus(apiKey.getStatus());
        response.setCreatedTime(apiKey.getCreatedTime());
        response.setUpdatedTime(apiKey.getUpdatedTime());

        return response;
    }
}
