package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.request.CreateProviderRequest;
import com.fsa.aicenter.application.dto.request.UpdateProviderRequest;
import com.fsa.aicenter.application.dto.response.ProviderResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
import com.fsa.aicenter.domain.model.valueobject.EntityStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提供商管理服务
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderManagementService {

    private final ProviderRepository providerRepository;

    /**
     * 创建提供商
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProvider(CreateProviderRequest request) {
        // 检查提供商编码是否已存在
        if (providerRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException(ErrorCode.PROVIDER_CODE_EXISTS);
        }

        // 构建领域对象
        Provider provider = new Provider();
        provider.setCode(request.getCode());
        provider.setName(request.getName());
        provider.setType(request.getType());
        provider.setBaseUrl(request.getBaseUrl());
        provider.setProtocolType(request.getProtocolType());
        provider.setChatEndpoint(request.getChatEndpoint());
        provider.setEmbeddingEndpoint(request.getEmbeddingEndpoint());
        provider.setImageEndpoint(request.getImageEndpoint());
        provider.setVideoEndpoint(request.getVideoEndpoint());
        provider.setExtraHeaders(request.getExtraHeaders());
        provider.setRequestTemplate(request.getRequestTemplate());
        provider.setResponseMapping(request.getResponseMapping());
        provider.setAuthType(request.getAuthType());
        provider.setAuthHeader(request.getAuthHeader());
        provider.setAuthPrefix(request.getAuthPrefix());
        provider.setApiKeyRequired(request.getApiKeyRequired());
        provider.setDescription(request.getDescription());
        provider.setSortOrder(request.getSortOrder());
        provider.setStatus(request.getStatus() != null ? request.getStatus() : EntityStatus.ENABLED);

        // 保存
        Provider savedProvider = providerRepository.save(provider);
        log.info("创建提供商成功: id={}, code={}", savedProvider.getId(), savedProvider.getCode());

        return savedProvider.getId();
    }

    /**
     * 更新提供商
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProvider(Long id, UpdateProviderRequest request) {
        Provider provider = providerRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        // 更新字段
        provider.setName(request.getName());
        provider.setBaseUrl(request.getBaseUrl());
        provider.setProtocolType(request.getProtocolType());
        provider.setChatEndpoint(request.getChatEndpoint());
        provider.setEmbeddingEndpoint(request.getEmbeddingEndpoint());
        provider.setImageEndpoint(request.getImageEndpoint());
        provider.setVideoEndpoint(request.getVideoEndpoint());
        provider.setExtraHeaders(request.getExtraHeaders());
        provider.setRequestTemplate(request.getRequestTemplate());
        provider.setResponseMapping(request.getResponseMapping());
        provider.setAuthType(request.getAuthType());
        provider.setAuthHeader(request.getAuthHeader());
        provider.setAuthPrefix(request.getAuthPrefix());
        provider.setApiKeyRequired(request.getApiKeyRequired());
        provider.setDescription(request.getDescription());
        provider.setSortOrder(request.getSortOrder());
        if (request.getStatus() != null) {
            provider.setStatus(request.getStatus());
        }

        providerRepository.update(provider);
        log.info("更新提供商成功: id={}, code={}", provider.getId(), provider.getCode());
    }

    /**
     * 删除提供商（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProvider(Long id) {
        Provider provider = providerRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        providerRepository.delete(id);
        log.info("删除提供商成功: id={}, code={}", provider.getId(), provider.getCode());
    }

    /**
     * 启用/禁用提供商
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleProviderStatus(Long id) {
        Provider provider = providerRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        EntityStatus newStatus = provider.isEnabled() ? EntityStatus.DISABLED : EntityStatus.ENABLED;
        provider.setStatus(newStatus);
        providerRepository.update(provider);

        log.info("切换提供商状态: id={}, status={}", provider.getId(), newStatus);
    }

    /**
     * 查询提供商详情
     */
    public ProviderResponse getProvider(Long id) {
        Provider provider = providerRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));
        return toResponse(provider);
    }

    /**
     * 查询所有提供商
     */
    public List<ProviderResponse> listProviders() {
        List<Provider> providers = providerRepository.findAll();
        return providers.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 领域对象转响应DTO
     */
    private ProviderResponse toResponse(Provider provider) {
        ProviderResponse response = new ProviderResponse();
        response.setId(provider.getId());
        response.setCode(provider.getCode());
        response.setName(provider.getName());
        response.setType(provider.getType());
        response.setBaseUrl(provider.getBaseUrl());
        response.setProtocolType(provider.getProtocolType());
        response.setChatEndpoint(provider.getChatEndpoint());
        response.setEmbeddingEndpoint(provider.getEmbeddingEndpoint());
        response.setImageEndpoint(provider.getImageEndpoint());
        response.setVideoEndpoint(provider.getVideoEndpoint());
        response.setExtraHeaders(provider.getExtraHeaders());
        response.setRequestTemplate(provider.getRequestTemplate());
        response.setResponseMapping(provider.getResponseMapping());
        response.setAuthType(provider.getAuthType());
        response.setAuthHeader(provider.getAuthHeader());
        response.setAuthPrefix(provider.getAuthPrefix());
        response.setApiKeyRequired(provider.getApiKeyRequired());
        response.setDescription(provider.getDescription());
        response.setSortOrder(provider.getSortOrder());
        response.setStatus(provider.getStatus());
        response.setCreatedTime(provider.getCreatedTime());
        response.setUpdatedTime(provider.getUpdatedTime());
        return response;
    }
}
