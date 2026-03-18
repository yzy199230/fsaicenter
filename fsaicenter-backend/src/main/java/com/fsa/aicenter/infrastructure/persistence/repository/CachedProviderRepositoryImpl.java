package com.fsa.aicenter.infrastructure.persistence.repository;

import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import com.fsa.aicenter.domain.model.valueobject.ProviderType;
import com.fsa.aicenter.infrastructure.persistence.entity.ProviderPO;
import com.fsa.aicenter.infrastructure.persistence.mapper.ProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class CachedProviderRepositoryImpl implements ProviderRepository {

    private final ProviderMapper providerMapper;

    private static final String CACHE_NAME = "provider";

    @Override
    @Cacheable(value = CACHE_NAME, key = "#id")
    public Optional<Provider> findById(Long id) {
        log.debug("从数据库查询Provider: id={}", id);
        ProviderPO po = providerMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'code:' + #code")
    public Optional<Provider> findByCode(String code) {
        log.debug("从数据库查询Provider: code={}", code);
        ProviderPO po = providerMapper.selectByCode(code);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'all'")
    public List<Provider> findAll() {
        log.debug("从数据库查询所有Provider");
        List<ProviderPO> poList = providerMapper.selectList(null);
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'enabled'")
    public List<Provider> findEnabled() {
        log.debug("从数据库查询启用的Provider");
        List<ProviderPO> poList = providerMapper.selectEnabled(EntityStatus.ENABLED.getCode());
        return poList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public Provider save(Provider provider) {
        log.debug("保存Provider并清除缓存: code={}", provider.getCode());
        ProviderPO po = toPO(provider);
        providerMapper.insert(po);
        provider.setId(po.getId());
        return provider;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void update(Provider provider) {
        log.debug("更新Provider并清除缓存: code={}", provider.getCode());
        ProviderPO po = toPO(provider);
        providerMapper.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void delete(Long id) {
        log.debug("删除Provider并清除缓存: id={}", id);
        providerMapper.deleteById(id);
    }

    private Provider toDomain(ProviderPO po) {
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