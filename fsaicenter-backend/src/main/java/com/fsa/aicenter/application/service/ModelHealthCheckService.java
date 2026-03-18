package com.fsa.aicenter.application.service;

import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.ModelApiKey;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.repository.ModelApiKeyRepository;
import com.fsa.aicenter.domain.model.repository.ModelRepository;
import com.fsa.aicenter.domain.model.repository.ProviderRepository;
import com.fsa.aicenter.domain.model.valueobject.HealthStatus;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.infrastructure.adapter.common.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模型健康检查服务
 * <p>
 * 定时检测模型API Key的可用性，自动屏蔽不可用的Key，
 * 恢复后自动重新启用。
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelHealthCheckService {

    private final ModelRepository modelRepository;
    private final ModelApiKeyRepository modelApiKeyRepository;
    private final ProviderRepository providerRepository;
    private final AiProviderAdapterFactory adapterFactory;

    /**
     * 健康检查超时时间（秒）
     */
    @Value("${ai.health-check.timeout:30}")
    private int healthCheckTimeout;

    /**
     * 是否启用健康检查
     */
    @Value("${ai.health-check.enabled:true}")
    private boolean healthCheckEnabled;

    /**
     * 连续失败多少次后标记为不健康
     */
    @Value("${ai.health-check.fail-threshold:3}")
    private int failThreshold;

    /**
     * 定时健康检查任务
     * <p>
     * 默认每5分钟执行一次，检查所有启用的模型的API Key健康状态
     * </p>
     */
    @Scheduled(fixedDelayString = "${ai.health-check.interval:300000}")
    public void scheduledHealthCheck() {
        if (!healthCheckEnabled) {
            log.debug("健康检查已禁用，跳过执行");
            return;
        }

        log.info("开始执行模型健康检查任务...");
        long startTime = System.currentTimeMillis();

        try {
            HealthCheckResult result = performHealthCheck();
            long duration = System.currentTimeMillis() - startTime;

            log.info("模型健康检查完成: 检查{}个Key, 健康{}个, 异常{}个, 跳过{}个, 耗时{}ms",
                    result.total, result.healthy, result.unhealthy, result.skipped, duration);
        } catch (Exception e) {
            log.error("模型健康检查任务执行失败", e);
        }
    }

    /**
     * 执行健康检查
     *
     * @return 检查结果统计
     */
    @CacheEvict(value = "model:keys", allEntries = true)
    public HealthCheckResult performHealthCheck() {
        HealthCheckResult result = new HealthCheckResult();

        // 获取所有启用的模型
        List<AiModel> models = modelRepository.findAll().stream()
                .filter(AiModel::isEnabled)
                .toList();

        log.debug("找到 {} 个启用的模型需要检查", models.size());

        for (AiModel model : models) {
            try {
                checkModelKeys(model, result);
            } catch (Exception e) {
                log.error("检查模型 {} 的Key时发生错误", model.getCode(), e);
            }
        }

        return result;
    }

    /**
     * 检查单个模型的所有API Key
     */
    private void checkModelKeys(AiModel model, HealthCheckResult result) {
        // 获取模型的所有Key
        List<ModelApiKey> keys = modelApiKeyRepository.findByModelId(model.getId());

        if (keys.isEmpty()) {
            log.debug("模型 {} 没有配置API Key，跳过检查", model.getCode());
            return;
        }

        // 获取提供商信息
        Optional<Provider> providerOpt = providerRepository.findById(model.getProviderId());
        if (providerOpt.isEmpty()) {
            log.warn("模型 {} 的提供商不存在，跳过检查", model.getCode());
            return;
        }

        Provider provider = providerOpt.get();

        // 检查适配器是否存在
        if (!adapterFactory.hasAdapter(provider.getCode())) {
            log.debug("提供商 {} 没有对应的适配器，跳过检查", provider.getCode());
            return;
        }

        for (ModelApiKey key : keys) {
            result.total++;

            // 跳过已禁用的Key
            if (key.getHealthStatus() == HealthStatus.DISABLED) {
                result.skipped++;
                log.debug("Key {} 已禁用，跳过检查", key.getKeyName());
                continue;
            }

            try {
                boolean healthy = checkSingleKey(model, provider, key);

                if (healthy) {
                    result.healthy++;
                    handleHealthyKey(key);
                } else {
                    result.unhealthy++;
                    handleUnhealthyKey(key);
                }
            } catch (Exception e) {
                result.unhealthy++;
                log.warn("检查Key {} 时发生异常: {}", key.getKeyName(), e.getMessage());
                handleUnhealthyKey(key);
            }
        }
    }

    /**
     * 检查单个API Key的健康状态
     *
     * @return true表示健康
     */
    private boolean checkSingleKey(AiModel model, Provider provider, ModelApiKey key) {
        log.debug("检查Key: model={}, key={}", model.getCode(), key.getKeyName());

        try {
            AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());

            // 构建简单的测试请求
            AiRequest testRequest = buildTestRequest(model);

            // 执行测试调用（带超时）
            Mono<AiResponse> responseMono = adapter.call(model, testRequest);
            AiResponse response = responseMono.block(Duration.ofSeconds(healthCheckTimeout));

            // 检查响应是否有效
            boolean healthy = response != null && response.getContent() != null;

            log.debug("Key {} 健康检查结果: {}", key.getKeyName(), healthy ? "健康" : "异常");
            return healthy;

        } catch (Exception e) {
            log.debug("Key {} 健康检查失败: {}", key.getKeyName(), e.getMessage());
            return false;
        }
    }

    /**
     * 构建测试请求
     * <p>
     * 根据模型类型构建不同的测试请求
     * </p>
     */
    private AiRequest buildTestRequest(AiModel model) {
        AiRequest.AiRequestBuilder builder = AiRequest.builder()
                .model(model.getCode());

        // 根据模型类型构建不同的测试请求
        if (model.getType() == ModelType.CHAT) {
            builder.messages(List.of(
                    Message.builder()
                            .role("user")
                            .content("Hi")
                            .build()
            ));
            builder.maxTokens(5);  // 限制token数量以减少成本
        } else if (model.getType() == ModelType.EMBEDDING) {
            builder.input("test");
        } else {
            // 其他类型使用简单的chat请求
            builder.messages(List.of(
                    Message.builder()
                            .role("user")
                            .content("Hi")
                            .build()
            ));
            builder.maxTokens(5);
        }

        return builder.build();
    }

    /**
     * 处理健康的Key
     */
    private void handleHealthyKey(ModelApiKey key) {
        // 如果之前是不健康状态，恢复为健康
        if (key.getHealthStatus() == HealthStatus.UNHEALTHY) {
            log.info("Key {} 恢复健康状态", key.getKeyName());
            key.setHealthStatus(HealthStatus.HEALTHY);
            key.setFailCount(0);
            modelApiKeyRepository.update(key);
        }
    }

    /**
     * 处理不健康的Key
     */
    private void handleUnhealthyKey(ModelApiKey key) {
        int newFailCount = (key.getFailCount() != null ? key.getFailCount() : 0) + 1;
        key.setFailCount(newFailCount);

        // 连续失败超过阈值，标记为不健康
        if (newFailCount >= failThreshold && key.getHealthStatus() == HealthStatus.HEALTHY) {
            log.warn("Key {} 连续失败 {} 次，标记为不健康", key.getKeyName(), newFailCount);
            key.setHealthStatus(HealthStatus.UNHEALTHY);
        }

        modelApiKeyRepository.update(key);
    }

    /**
     * 手动触发单个模型的健康检查
     *
     * @param modelId 模型ID
     * @return 检查结果
     */
    @Async
    @CacheEvict(value = "model:keys", key = "#modelId")
    public CompletableFuture<HealthCheckResult> checkModel(Long modelId) {
        HealthCheckResult result = new HealthCheckResult();

        Optional<AiModel> modelOpt = modelRepository.findById(modelId);
        if (modelOpt.isEmpty()) {
            log.warn("模型 {} 不存在", modelId);
            return CompletableFuture.completedFuture(result);
        }

        AiModel model = modelOpt.get();
        checkModelKeys(model, result);

        return CompletableFuture.completedFuture(result);
    }

    /**
     * 手动触发单个Key的健康检查
     *
     * @param keyId Key ID
     * @return true表示健康
     */
    @CacheEvict(value = "model:keys", allEntries = true)
    public boolean checkKey(Long keyId) {
        Optional<ModelApiKey> keyOpt = modelApiKeyRepository.findById(keyId);
        if (keyOpt.isEmpty()) {
            log.warn("Key {} 不存在", keyId);
            return false;
        }

        ModelApiKey key = keyOpt.get();

        Optional<AiModel> modelOpt = modelRepository.findById(key.getModelId());
        if (modelOpt.isEmpty()) {
            log.warn("Key {} 对应的模型不存在", keyId);
            return false;
        }

        AiModel model = modelOpt.get();

        Optional<Provider> providerOpt = providerRepository.findById(model.getProviderId());
        if (providerOpt.isEmpty()) {
            log.warn("模型 {} 的提供商不存在", model.getCode());
            return false;
        }

        Provider provider = providerOpt.get();

        boolean healthy = checkSingleKey(model, provider, key);

        if (healthy) {
            handleHealthyKey(key);
        } else {
            handleUnhealthyKey(key);
        }

        return healthy;
    }

    /**
     * 健康检查结果统计
     */
    public static class HealthCheckResult {
        public int total = 0;
        public int healthy = 0;
        public int unhealthy = 0;
        public int skipped = 0;

        @Override
        public String toString() {
            return String.format("HealthCheckResult{total=%d, healthy=%d, unhealthy=%d, skipped=%d}",
                    total, healthy, unhealthy, skipped);
        }
    }
}
