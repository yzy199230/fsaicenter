package com.fsa.aicenter.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.apikey.repository.ApiKeyRepository;
import com.fsa.aicenter.domain.apikey.valueobject.AccessControl;
import com.fsa.aicenter.domain.apikey.valueobject.Quota;
import com.fsa.aicenter.domain.apikey.valueobject.RateLimit;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.infrastructure.exception.RepositoryException;
import com.fsa.aicenter.infrastructure.persistence.entity.ApiKeyModelAccessPO;
import com.fsa.aicenter.infrastructure.persistence.entity.ApiKeyPO;
import com.fsa.aicenter.infrastructure.persistence.mapper.ApiKeyMapper;
import com.fsa.aicenter.infrastructure.persistence.mapper.ApiKeyModelAccessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API密钥仓储实现类
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ApiKeyRepositoryImpl implements ApiKeyRepository {

    private static final int ACCESS_TYPE_ALLOW = 1;
    private static final int ACCESS_TYPE_DENY = 0;
    private static final String DELIMITER = ",";

    private final ApiKeyMapper apiKeyMapper;
    private final ApiKeyModelAccessMapper modelAccessMapper;

    @Override
    public Optional<ApiKey> findById(Long id) {
        ApiKeyPO po = apiKeyMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<ApiKey> findByKeyValue(String keyValue) {
        ApiKeyPO po = apiKeyMapper.selectByKeyValue(keyValue);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ApiKey> findAll() {
        LambdaQueryWrapper<ApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiKeyPO::getIsDeleted, 0)
                .orderByDesc(ApiKeyPO::getCreatedTime);
        List<ApiKeyPO> poList = apiKeyMapper.selectList(wrapper);
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiKey> findActiveKeys() {
        List<ApiKeyPO> poList = apiKeyMapper.selectActiveKeys();
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ApiKey save(ApiKey apiKey) {
        log.info("Saving API key: name={}, keyValue={}", apiKey.getKeyName(),
            apiKey.getKeyValue().substring(0, 8) + "***");  // 脱敏显示

        ApiKeyPO po = toPO(apiKey);
        apiKeyMapper.insert(po);
        apiKey.setId(po.getId());

        log.info("API key saved with id={}", po.getId());
        return apiKey;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(ApiKey apiKey) {
        if (apiKey.getId() == null) {
            throw new IllegalArgumentException("API key ID cannot be null for update");
        }

        log.info("Updating API key: id={}, name={}", apiKey.getId(), apiKey.getKeyName());

        ApiKeyPO po = toPO(apiKey);
        int rows = apiKeyMapper.updateById(po);

        if (rows == 0) {
            log.error("API key not found for update: id={}", apiKey.getId());
            throw new RepositoryException("API key not found: " + apiKey.getId());
        }

        log.info("API key updated: id={}", apiKey.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        log.info("Deleting API key: id={}", id);

        int rows = apiKeyMapper.deleteById(id);

        if (rows == 0) {
            log.warn("API key not found for delete: id={}", id);
        } else {
            log.info("API key deleted: id={}", id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveModelAccess(Long apiKeyId, Long modelId, boolean allow) {
        log.debug("Saving model access: apiKeyId={}, modelId={}, allow={}", apiKeyId, modelId, allow);

        try {
            // 先尝试查询是否存在
            LambdaQueryWrapper<ApiKeyModelAccessPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiKeyModelAccessPO::getApiKeyId, apiKeyId)
                   .eq(ApiKeyModelAccessPO::getModelId, modelId)
                   .eq(ApiKeyModelAccessPO::getIsDeleted, 0);

            ApiKeyModelAccessPO existing = modelAccessMapper.selectOne(wrapper);

            if (existing != null) {
                // 更新access_type
                existing.setAccessType(allow ? ACCESS_TYPE_ALLOW : ACCESS_TYPE_DENY);
                modelAccessMapper.updateById(existing);
                log.debug("Updated model access for apiKeyId={}, modelId={}", apiKeyId, modelId);
            } else {
                // 新建记录
                ApiKeyModelAccessPO po = new ApiKeyModelAccessPO();
                po.setApiKeyId(apiKeyId);
                po.setModelId(modelId);
                po.setAccessType(allow ? ACCESS_TYPE_ALLOW : ACCESS_TYPE_DENY);
                modelAccessMapper.insert(po);
                log.debug("Inserted new model access for apiKeyId={}, modelId={}", apiKeyId, modelId);
            }
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发插入导致唯一约束冲突，重试更新
            log.warn("Duplicate key when inserting model access, retrying update: apiKeyId={}, modelId={}", apiKeyId, modelId);
            LambdaQueryWrapper<ApiKeyModelAccessPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiKeyModelAccessPO::getApiKeyId, apiKeyId)
                   .eq(ApiKeyModelAccessPO::getModelId, modelId);

            ApiKeyModelAccessPO existing = modelAccessMapper.selectOne(wrapper);
            if (existing != null) {
                existing.setAccessType(allow ? ACCESS_TYPE_ALLOW : ACCESS_TYPE_DENY);
                existing.setIsDeleted(0);  // 确保未被逻辑删除
                modelAccessMapper.updateById(existing);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteModelAccess(Long apiKeyId, Long modelId) {
        modelAccessMapper.deleteByApiKeyIdAndModelId(apiKeyId, modelId);
    }

    @Override
    public List<Long> findAccessibleModelIds(Long apiKeyId) {
        List<ApiKeyModelAccessPO> accessList = modelAccessMapper.selectByApiKeyId(apiKeyId);

        return accessList.stream()
            .filter(po -> po.getAccessType() == ACCESS_TYPE_ALLOW)  // 只返回允许访问的
            .map(ApiKeyModelAccessPO::getModelId)
            .collect(Collectors.toList());
    }

    // ========== 转换方法 ==========

    /**
     * PO转领域对象
     */
    private ApiKey toDomain(ApiKeyPO po) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(po.getId());
        apiKey.setKeyValue(po.getKeyValue());
        apiKey.setKeyName(po.getKeyName());
        apiKey.setDescription(po.getDescription());

        // 值对象转换
        apiKey.setQuota(new Quota(po.getQuotaTotal(), po.getQuotaUsed()));
        apiKey.setRateLimit(new RateLimit(po.getRateLimitPerMinute(), po.getRateLimitPerDay()));

        // AccessControl转换 - 使用静态工厂方法，添加异常处理
        try {
            apiKey.setAccessControl(AccessControl.fromStrings(
                po.getAllowedModelTypes(),
                po.getAllowedIpWhitelist()
            ));
        } catch (Exception e) {
            log.error("Invalid access control data for API key {}: modelTypes={}, ipWhitelist={}",
                po.getId(), po.getAllowedModelTypes(), po.getAllowedIpWhitelist(), e);
            throw new RepositoryException("Invalid access control format for API key: " + po.getId(), e);
        }

        apiKey.setExpireTime(po.getExpireTime());

        // 验证status字段
        if (po.getStatus() == null) {
            log.error("Invalid status for API key {}: null", po.getId());
            throw new RepositoryException("API key status cannot be null for key: " + po.getId());
        }
        apiKey.setStatus(EntityStatus.fromCode(po.getStatus()));

        apiKey.setCreatedTime(po.getCreatedTime());
        apiKey.setUpdatedTime(po.getUpdatedTime());

        return apiKey;
    }

    /**
     * 领域对象转PO
     */
    private ApiKeyPO toPO(ApiKey apiKey) {
        ApiKeyPO po = new ApiKeyPO();
        po.setId(apiKey.getId());
        po.setKeyValue(apiKey.getKeyValue());
        po.setKeyName(apiKey.getKeyName());
        po.setDescription(apiKey.getDescription());

        // 值对象字段展开
        if (apiKey.getQuota() != null) {
            po.setQuotaTotal(apiKey.getQuota().getTotal());
            po.setQuotaUsed(apiKey.getQuota().getUsed());
        }

        if (apiKey.getRateLimit() != null) {
            po.setRateLimitPerMinute(apiKey.getRateLimit().getPerMinute());
            po.setRateLimitPerDay(apiKey.getRateLimit().getPerDay());
        }

        // AccessControl转换为逗号分隔字符串
        if (apiKey.getAccessControl() != null) {
            po.setAllowedModelTypes(formatModelTypes(apiKey.getAccessControl().getAllowedModelTypes()));
            po.setAllowedIpWhitelist(formatIpWhitelist(apiKey.getAccessControl().getAllowedIpWhitelist()));
        }

        po.setExpireTime(apiKey.getExpireTime());
        if (apiKey.getStatus() != null) {
            po.setStatus(apiKey.getStatus().getCode());
        }
        po.setCreatedTime(apiKey.getCreatedTime());
        po.setUpdatedTime(apiKey.getUpdatedTime());

        return po;
    }

    // ========== 辅助方法 ==========

    /**
     * 格式化ModelType为逗号分隔字符串
     */
    private String formatModelTypes(Set<ModelType> modelTypes) {
        if (modelTypes == null || modelTypes.isEmpty()) {
            return null;
        }
        return modelTypes.stream()
                .map(ModelType::getCode)
                .collect(Collectors.joining(DELIMITER));
    }

    /**
     * 格式化IP白名单
     */
    private String formatIpWhitelist(Set<String> ipWhitelist) {
        if (ipWhitelist == null || ipWhitelist.isEmpty()) {
            return null;
        }
        return String.join(DELIMITER, ipWhitelist);
    }
}
