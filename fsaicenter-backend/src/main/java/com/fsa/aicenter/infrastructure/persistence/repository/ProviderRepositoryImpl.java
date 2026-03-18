package com.fsa.aicenter.infrastructure.persistence.repository;

import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ProviderType;
import com.fsa.aicenter.infrastructure.persistence.entity.ProviderPO;
import com.fsa.aicenter.infrastructure.persistence.mapper.ProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 提供商仓储实现类
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProviderRepositoryImpl implements ProviderRepository {

    private final ProviderMapper providerMapper;

    @Override
    public Optional<Provider> findById(Long id) {
        ProviderPO po = providerMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<Provider> findByCode(String code) {
        ProviderPO po = providerMapper.selectByCode(code);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Provider> findAll() {
        List<ProviderPO> poList = providerMapper.selectList(null);
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Provider> findEnabled() {
        List<ProviderPO> poList = providerMapper.selectEnabled(EntityStatus.ENABLED.getCode());
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Provider save(Provider provider) {
        ProviderPO po = toPO(provider);
        providerMapper.insert(po);
        provider.setId(po.getId());
        return provider;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(Provider provider) {
        ProviderPO po = toPO(provider);
        providerMapper.updateById(po);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        providerMapper.deleteById(id);
    }

    // ========== 转换方法 ==========

    /**
     * PO转领域对象
     */
    private Provider toDomain(ProviderPO po) {
        // 验证必需的枚举字段
        if (po.getProviderType() == null || po.getProviderType().trim().isEmpty()) {
            log.error("Invalid provider type for provider {}: null or empty", po.getId());
            throw new IllegalStateException("Provider type cannot be null for provider: " + po.getId());
        }
        if (po.getStatus() == null) {
            log.error("Invalid status for provider {}: null", po.getId());
            throw new IllegalStateException("Provider status cannot be null for provider: " + po.getId());
        }

        Provider provider = new Provider();
        provider.setId(po.getId());
        provider.setCode(po.getProviderCode());
        provider.setName(po.getProviderName());
        provider.setType(ProviderType.fromCode(po.getProviderType()));
        provider.setBaseUrl(po.getBaseUrl());
        provider.setProtocolType(po.getProtocolType());
        provider.setChatEndpoint(po.getChatEndpoint());
        provider.setEmbeddingEndpoint(po.getEmbeddingEndpoint());
        provider.setImageEndpoint(po.getImageEndpoint());
        provider.setVideoEndpoint(po.getVideoEndpoint());
        provider.setExtraHeaders(po.getExtraHeaders());
        provider.setRequestTemplate(po.getRequestTemplate());
        provider.setResponseMapping(po.getResponseMapping());
        provider.setAuthType(po.getAuthType());
        provider.setAuthHeader(po.getAuthHeader());
        provider.setAuthPrefix(po.getAuthPrefix());
        provider.setApiKeyRequired(po.getApiKeyRequired());
        provider.setDescription(po.getDescription());
        provider.setSortOrder(po.getSortOrder());
        provider.setStatus(EntityStatus.fromCode(po.getStatus()));
        provider.setCreatedTime(po.getCreatedTime());
        provider.setUpdatedTime(po.getUpdatedTime());
        return provider;
    }

    /**
     * 领域对象转PO
     */
    private ProviderPO toPO(Provider provider) {
        ProviderPO po = new ProviderPO();
        po.setId(provider.getId());
        po.setProviderCode(provider.getCode());
        po.setProviderName(provider.getName());
        po.setProviderType(provider.getType().getCode());
        po.setBaseUrl(provider.getBaseUrl());
        po.setProtocolType(provider.getProtocolType());
        po.setChatEndpoint(provider.getChatEndpoint());
        po.setEmbeddingEndpoint(provider.getEmbeddingEndpoint());
        po.setImageEndpoint(provider.getImageEndpoint());
        po.setVideoEndpoint(provider.getVideoEndpoint());
        po.setExtraHeaders(provider.getExtraHeaders());
        po.setRequestTemplate(provider.getRequestTemplate());
        po.setResponseMapping(provider.getResponseMapping());
        po.setAuthType(provider.getAuthType());
        po.setAuthHeader(provider.getAuthHeader());
        po.setAuthPrefix(provider.getAuthPrefix());
        po.setApiKeyRequired(provider.getApiKeyRequired());
        po.setDescription(provider.getDescription());
        po.setSortOrder(provider.getSortOrder());
        po.setStatus(provider.getStatus().getCode());
        po.setCreatedTime(provider.getCreatedTime());
        po.setUpdatedTime(provider.getUpdatedTime());
        return po;
    }
}
