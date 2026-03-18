package com.fsa.aicenter.infrastructure.adapter;

import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
import com.fsa.aicenter.infrastructure.adapter.common.AiProviderAdapter;
import com.fsa.aicenter.infrastructure.adapter.generic.GenericOpenAiAdapter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI提供商适配器工厂
 * <p>
 * 负责管理所有AI提供商适配器的注册和获取。
 * 通过Spring自动发现和注册所有AiProviderAdapter实现类。
 * </p>
 *
 * @author FSA AI Center
 */
@Component
public class AdapterFactory {

    private static final Logger log = LoggerFactory.getLogger(AdapterFactory.class);

    /**
     * 适配器注册表：providerCode -> Adapter实例
     */
    private final Map<String, AiProviderAdapter> adapterRegistry = new ConcurrentHashMap<>();

    /**
     * 自动注入所有AiProviderAdapter实现类
     */
    @Autowired(required = false)
    private List<AiProviderAdapter> adapters;

    /**
     * 提供商仓储
     */
    private final ProviderRepository providerRepository;

    private final GenericOpenAiAdapter genericAdapter;

    public AdapterFactory(ProviderRepository providerRepository, GenericOpenAiAdapter genericAdapter) {
        this.providerRepository = providerRepository;
        this.genericAdapter = genericAdapter;
    }

    /**
     * 初始化：注册所有适配器
     */
    @PostConstruct
    public void init() {
        if (adapters == null || adapters.isEmpty()) {
            log.warn("No AiProviderAdapter implementations found");
            return;
        }

        for (AiProviderAdapter adapter : adapters) {
            String providerCode = adapter.getProviderCode();
            if (!"__generic__".equals(providerCode)) {
                adapterRegistry.put(providerCode, adapter);
                log.info("Registered AI provider adapter: {} (type={})",
                        providerCode, adapter.getProviderType());
            }
        }

        log.info("Total {} dedicated adapters registered, generic adapter ready", adapterRegistry.size());
    }

    /**
     * 根据模型获取对应的适配器
     *
     * @param model AI模型聚合根
     * @return 对应的适配器
     * @throws IllegalArgumentException 如果找不到对应的适配器
     */
    public AiProviderAdapter getAdapter(AiModel model) {
        if (model == null) {
            throw new IllegalArgumentException("AI model cannot be null");
        }

        String providerCode = getProviderCode(model);

        AiProviderAdapter adapter = adapterRegistry.get(providerCode);
        if (adapter != null) {
            log.debug("使用专用适配器: {}", providerCode);
            return adapter;
        }

        Provider provider = providerRepository.findByCode(providerCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVIDER_NOT_FOUND));

        if (genericAdapter.supports(provider)) {
            log.debug("使用通用适配器: {}", providerCode);
            return genericAdapter;
        }

        throw new BusinessException(ErrorCode.ADAPTER_NOT_FOUND,
                "No adapter found for provider: " + providerCode);
    }

    /**
     * 根据提供商代码获取适配器
     *
     * @param providerCode 提供商代码
     * @return 对应的适配器
     * @throws IllegalArgumentException 如果找不到对应的适配器
     */
    public AiProviderAdapter getAdapter(String providerCode) {
        if (providerCode == null || providerCode.isEmpty()) {
            throw new IllegalArgumentException("Provider code cannot be null or empty");
        }

        AiProviderAdapter adapter = adapterRegistry.get(providerCode);
        if (adapter == null) {
            throw new IllegalArgumentException(
                    String.format("No adapter found for provider: %s", providerCode));
        }

        return adapter;
    }

    /**
     * 获取所有已注册的适配器
     *
     * @return 所有适配器
     */
    public Map<String, AiProviderAdapter> getAllAdapters() {
        return Map.copyOf(adapterRegistry);
    }

    /**
     * 从模型中获取提供商代码
     *
     * @param model AI模型
     * @return 提供商代码
     * @throws BusinessException 如果找不到对应的提供商
     */
    private String getProviderCode(AiModel model) {
        Long providerId = model.getProviderId();
        if (providerId == null) {
            log.error("Model {} has no providerId", model.getCode());
            throw new BusinessException(ErrorCode.PROVIDER_NOT_FOUND,
                    "Provider ID is null for model: " + model.getCode());
        }

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.error("Provider not found for id: {}", providerId);
                    return new BusinessException(ErrorCode.PROVIDER_NOT_FOUND,
                            "Provider not found: " + providerId);
                });

        if (!provider.isEnabled()) {
            log.warn("Provider {} is disabled", provider.getCode());
            throw new BusinessException(ErrorCode.PROVIDER_DISABLED,
                    "Provider is disabled: " + provider.getCode());
        }

        return provider.getCode();
    }
}
