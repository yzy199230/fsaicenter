package com.fsa.aicenter.infrastructure.adapter.test;

import com.fsa.aicenter.application.dto.request.TestModelRequest;
import com.fsa.aicenter.application.dto.response.EmbeddingStats;
import com.fsa.aicenter.application.dto.response.TestModelResponse;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.infrastructure.adapter.AdapterFactory;
import com.fsa.aicenter.infrastructure.adapter.common.AiProviderAdapter;
import com.fsa.aicenter.infrastructure.adapter.common.AiRequest;
import com.fsa.aicenter.infrastructure.adapter.common.AiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向量模型测试策略
 *
 * @author FSA AI Center
 */
@Component
public class EmbeddingModelTestStrategy implements ModelTestStrategy {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingModelTestStrategy.class);

    private final AdapterFactory adapterFactory;

    public EmbeddingModelTestStrategy(AdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ModelType getSupportedType() {
        return ModelType.EMBEDDING;
    }

    @Override
    public TestModelResponse test(AiModel model, Provider provider, TestModelRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建AI请求
            AiRequest aiRequest = AiRequest.builder()
                    .model(model.getCode())
                    .input(request.getText())
                    .build();

            // 获取适配器并调用
            AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());
            AiResponse aiResponse = adapter.embedding(model, aiRequest).block();

            long duration = System.currentTimeMillis() - startTime;

            // 计算统计信息
            List<Double> embedding = aiResponse.getEmbedding();
            EmbeddingStats stats = calculateStats(embedding);

            return TestModelResponse.builder()
                    .success(true)
                    .duration(duration)
                    .embeddingStats(stats)
                    .inputTokens(aiResponse.getPromptTokens())
                    .build();

        } catch (Exception e) {
            log.error("Embedding model test failed: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .duration(duration)
                    .build();
        }
    }

    /**
     * 计算向量统计信息
     */
    private EmbeddingStats calculateStats(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return null;
        }

        int dimensions = embedding.size();
        double min = embedding.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = embedding.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double mean = embedding.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // 计算标准差
        double variance = embedding.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        return EmbeddingStats.builder()
                .dimensions(dimensions)
                .min(min)
                .max(max)
                .mean(mean)
                .stdDev(stdDev)
                .build();
    }
}
