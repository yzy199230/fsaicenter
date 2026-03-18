package com.fsa.aicenter.infrastructure.adapter.common;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI提供商适配器工厂
 * <p>
 * 负责管理和获取AI提供商适配器实例。
 * 通过Spring自动注入所有实现了AiProviderAdapter接口的Bean，
 * 并按提供商代码建立索引，实现快速查找。
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * {@code
 * AiProviderAdapter adapter = adapterFactory.getAdapter("openai");
 * AiResponse response = adapter.call(model, request);
 * }
 * </pre>
 * </p>
 */
@Slf4j
@Component
public class AiProviderAdapterFactory {

    /**
     * 适配器缓存
     * Key: 提供商代码（如 openai, qwen, wenxin, ollama）
     * Value: 对应的适配器实例
     */
    private final Map<String, AiProviderAdapter> adapterMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，自动注册所有适配器
     *
     * @param adapters Spring自动注入的所有适配器实现
     */
    public AiProviderAdapterFactory(List<AiProviderAdapter> adapters) {
        if (adapters == null || adapters.isEmpty()) {
            log.warn("No AI provider adapters found, adapter factory will be empty");
            return;
        }

        for (AiProviderAdapter adapter : adapters) {
            String providerCode = adapter.getProviderCode();
            if (providerCode == null || providerCode.isBlank()) {
                log.warn("Adapter {} has null or blank provider code, skipping registration",
                        adapter.getClass().getName());
                continue;
            }

            if (adapterMap.containsKey(providerCode)) {
                log.error("Duplicate provider code: {}, existing: {}, new: {}",
                        providerCode,
                        adapterMap.get(providerCode).getClass().getName(),
                        adapter.getClass().getName());
                throw new IllegalStateException(
                        "Duplicate AI provider adapter for code: " + providerCode);
            }

            adapterMap.put(providerCode, adapter);
            log.info("Registered AI provider adapter: {} -> {}",
                    providerCode, adapter.getClass().getSimpleName());
        }

        log.info("AI provider adapter factory initialized with {} adapters", adapterMap.size());
    }

    /**
     * 根据提供商代码获取适配器
     *
     * @param providerCode 提供商代码（如 openai, qwen, wenxin, ollama）
     * @return 对应的适配器实例
     * @throws IllegalArgumentException 如果找不到对应的适配器
     */
    public AiProviderAdapter getAdapter(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new IllegalArgumentException("Provider code cannot be null or blank");
        }

        AiProviderAdapter adapter = adapterMap.get(providerCode);
        if (adapter == null) {
            throw new IllegalArgumentException(
                    "No adapter found for provider code: " + providerCode +
                    ", available providers: " + adapterMap.keySet());
        }

        return adapter;
    }

    /**
     * 检查是否存在指定提供商的适配器
     *
     * @param providerCode 提供商代码
     * @return true表示存在
     */
    public boolean hasAdapter(String providerCode) {
        return providerCode != null && adapterMap.containsKey(providerCode);
    }

    /**
     * 获取所有已注册的提供商代码
     *
     * @return 提供商代码集合
     */
    public java.util.Set<String> getAvailableProviders() {
        return java.util.Collections.unmodifiableSet(adapterMap.keySet());
    }

    /**
     * 获取已注册的适配器数量
     *
     * @return 适配器数量
     */
    public int getAdapterCount() {
        return adapterMap.size();
    }
}
