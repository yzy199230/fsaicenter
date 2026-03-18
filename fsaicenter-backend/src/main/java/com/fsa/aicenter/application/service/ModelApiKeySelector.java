package com.fsa.aicenter.application.service;

import com.fsa.aicenter.domain.model.entity.ModelApiKey;
import com.fsa.aicenter.domain.model.repository.ModelApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模型API Key选择服务
 * <p>
 * 负责从Key池中选择可用的Key，支持多种负载均衡策略
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelApiKeySelector {

    private final ModelApiKeyRepository modelApiKeyRepository;

    // 轮询计数器（针对每个模型）
    private static final java.util.Map<Long, AtomicInteger> ROUND_ROBIN_COUNTER = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 为模型选择一个可用的API Key
     *
     * @param modelId 模型ID
     * @param strategy 选择策略：ROUND_ROBIN（轮询）、LEAST_USED（最少使用）、WEIGHTED（加权）
     * @return 可用的Key，如果没有可用Key返回Optional.empty()
     */
    public Optional<ModelApiKey> selectKey(Long modelId, SelectStrategy strategy) {
        List<ModelApiKey> availableKeys = getAvailableKeys(modelId);

        if (availableKeys.isEmpty()) {
            log.warn("模型 {} 没有可用的API Key", modelId);
            return Optional.empty();
        }

        ModelApiKey selected = switch (strategy) {
            case ROUND_ROBIN -> selectByRoundRobin(modelId, availableKeys);
            case LEAST_USED -> selectByLeastUsed(availableKeys);
            case WEIGHTED -> selectByWeighted(modelId, availableKeys);
        };

        log.debug("为模型 {} 选择了Key: {} (策略: {})", modelId, selected.getKeyName(), strategy);
        return Optional.of(selected);
    }

    /**
     * 默认选择策略（加权轮询）
     */
    public Optional<ModelApiKey> selectKey(Long modelId) {
        return selectKey(modelId, SelectStrategy.WEIGHTED);
    }

    /**
     * 获取模型所有可用的Key
     */
    @Cacheable(value = "model:keys", key = "#modelId", unless = "#result.isEmpty()")
    public List<ModelApiKey> getAvailableKeys(Long modelId) {
        return modelApiKeyRepository.findAvailableKeysByModelId(modelId);
    }

    /**
     * 轮询策略
     */
    private ModelApiKey selectByRoundRobin(Long modelId, List<ModelApiKey> keys) {
        AtomicInteger counter = ROUND_ROBIN_COUNTER.computeIfAbsent(modelId, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % keys.size();
        if (counter.get() > 10000) {
            counter.set(0); // 防止溢出
        }
        return keys.get(index);
    }

    /**
     * 最少使用策略
     */
    private ModelApiKey selectByLeastUsed(List<ModelApiKey> keys) {
        return keys.stream()
                .min(Comparator.comparing(k -> k.getTotalRequests() == null ? 0L : k.getTotalRequests()))
                .orElseThrow();
    }

    /**
     * 加权策略（结合权重和轮询）
     */
    private ModelApiKey selectByWeighted(Long modelId, List<ModelApiKey> keys) {
        // 计算总权重
        int totalWeight = keys.stream()
                .mapToInt(k -> k.getWeight() == null ? 1 : k.getWeight())
                .sum();

        // 获取轮询计数
        AtomicInteger counter = ROUND_ROBIN_COUNTER.computeIfAbsent(modelId, k -> new AtomicInteger(0));
        int randomPoint = counter.getAndIncrement() % totalWeight;
        if (counter.get() > 10000) {
            counter.set(0);
        }

        // 根据权重选择
        int currentWeight = 0;
        for (ModelApiKey key : keys) {
            int weight = key.getWeight() == null ? 1 : key.getWeight();
            currentWeight += weight;
            if (randomPoint < currentWeight) {
                return key;
            }
        }

        return keys.get(0); // fallback
    }

    /**
     * 记录Key使用成功
     */
    public void recordSuccess(Long keyId) {
        try {
            modelApiKeyRepository.recordSuccess(keyId);
        } catch (Exception e) {
            log.error("记录Key使用成功失败: {}", keyId, e);
        }
    }

    /**
     * 记录Key使用失败
     */
    public void recordFailure(Long keyId) {
        try {
            modelApiKeyRepository.recordFailure(keyId);
        } catch (Exception e) {
            log.error("记录Key使用失败失败: {}", keyId, e);
        }
    }

    /**
     * 消费配额
     */
    public boolean consumeQuota(Long keyId, long amount) {
        try {
            return modelApiKeyRepository.consumeQuota(keyId, amount);
        } catch (Exception e) {
            log.error("消费配额失败: {}", keyId, e);
            return false;
        }
    }

    /**
     * Key选择策略枚举
     */
    public enum SelectStrategy {
        ROUND_ROBIN,  // 轮询
        LEAST_USED,   // 最少使用
        WEIGHTED      // 加权
    }
}
