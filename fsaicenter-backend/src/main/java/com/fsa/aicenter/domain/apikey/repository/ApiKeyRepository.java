package com.fsa.aicenter.domain.apikey.repository;

import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import java.util.List;
import java.util.Optional;

/**
 * API密钥仓储接口
 */
public interface ApiKeyRepository {
    Optional<ApiKey> findById(Long id);
    Optional<ApiKey> findByKeyValue(String keyValue);
    List<ApiKey> findAll();
    List<ApiKey> findActiveKeys();
    ApiKey save(ApiKey apiKey);
    void update(ApiKey apiKey);
    void delete(Long id);

    // 模型访问权限管理
    void saveModelAccess(Long apiKeyId, Long modelId, boolean allow);
    void deleteModelAccess(Long apiKeyId, Long modelId);
    List<Long> findAccessibleModelIds(Long apiKeyId);
    void syncModelAccess(Long apiKeyId, java.util.Set<Long> allowedModelIds);
}
