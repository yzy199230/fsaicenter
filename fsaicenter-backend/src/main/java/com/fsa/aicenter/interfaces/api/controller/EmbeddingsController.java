package com.fsa.aicenter.interfaces.api.controller;

import com.fsa.aicenter.application.service.AiProxyService;
import com.fsa.aicenter.application.service.QuotaManager;
import com.fsa.aicenter.common.model.Result;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.log.valueobject.RequestType;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.infrastructure.adapter.common.AiRequest;
import com.fsa.aicenter.infrastructure.adapter.common.AiResponse;
import com.fsa.aicenter.infrastructure.event.DomainEventPublisher;
import com.fsa.aicenter.interfaces.api.dto.Embedding;
import com.fsa.aicenter.interfaces.api.dto.EmbeddingsRequest;
import com.fsa.aicenter.interfaces.api.dto.EmbeddingsResponse;
import com.fsa.aicenter.interfaces.api.dto.Usage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Embeddings API控制器
 * <p>
 * 提供文本向量化服务的API接口。
 * </p>
 *
 * @author FSA AI Center
 */
@Tag(name = "AI向量化API", description = "Embeddings接口")
@RestController
@RequestMapping("/api/v1/ai/embeddings")
public class EmbeddingsController {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingsController.class);

    @Autowired
    private AiProxyService aiProxyService;

    @Autowired
    private QuotaManager quotaManager;

    @Autowired
    private DomainEventPublisher eventPublisher;

    /**
     * 创建文本向量
     * <p>
     * 将文本转换为向量表示，支持单个文本或批量文本。
     * </p>
     *
     * @param request     向量化请求参数
     * @param httpRequest HTTP请求对象，从attribute获取已认证的ApiKey
     * @return 向量化响应结果
     */
    @Operation(summary = "文本向量化", description = "将文本转换为向量表示")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Result<EmbeddingsResponse>> createEmbeddings(
            @Valid @RequestBody EmbeddingsRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("收到向量化请求，模型: {}, 类型: {}, 编码格式: {}",
                request.getModel(), request.getModelType(), request.getEncodingFormat());

        // 从request.getAttribute获取已认证的ApiKey
        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("apiKey");
        if (apiKey == null) {
            log.error("ApiKey未找到，认证过滤器可能未正确设置");
            return Mono.just(Result.error("认证失败"));
        }

        // 记录请求开始时间
        java.time.LocalDateTime requestTime = java.time.LocalDateTime.now();
        String requestId = java.util.UUID.randomUUID().toString();

        // 预扣配额
        int estimatedTokens = estimateTokens(request);
        String preDeductId = quotaManager.preDeduct(apiKey, estimatedTokens);

        // 转换为AiRequest
        AiRequest aiRequest = toAiRequest(request);

        // 调用AiProxyService并获取实际使用的模型
        Mono<AiResponse> responseMono;
        AiModel selectedModel;
        if (request.getModel() != null && !request.getModel().isEmpty()) {
            // 指定模型，不降级
            selectedModel = aiProxyService.selectModelByCode(request.getModel());
            responseMono = aiProxyService.call(request.getModel(), aiRequest);
        } else {
            // 按类型选择，支持降级
            ModelType modelType = ModelType.fromCode(request.getModelType());
            java.util.List<AiModel> models = aiProxyService.selectModelsByType(modelType);
            selectedModel = models.get(0);
            responseMono = aiProxyService.callWithFallback(modelType, aiRequest);
        }

        return responseMono
                .map(aiResponse -> {
                    java.time.LocalDateTime responseTime = java.time.LocalDateTime.now();
                    long duration = java.time.Duration.between(requestTime, responseTime).toMillis();

                    // 确认配额扣减
                    int actualTokens = aiResponse.calculateTotalTokens();
                    quotaManager.confirm(preDeductId, actualTokens);

                    // 发布计费事件
                    eventPublisher.publishBillingEvent(
                            requestId,
                            apiKey,
                            selectedModel,
                            actualTokens,
                            requestTime,
                            responseTime
                    );

                    // 发布日志事件
                    eventPublisher.publishSuccessLogEvent(
                            requestId,
                            apiKey,
                            selectedModel,
                            "/api/v1/ai/embeddings",
                            RequestType.EMBEDDING,
                            false,
                            request,
                            aiResponse,
                            duration,
                            actualTokens,
                            aiResponse.getPromptTokens(),
                            null,
                            httpRequest.getRemoteAddr(),
                            httpRequest.getHeader("User-Agent")
                    );

                    EmbeddingsResponse embeddingsResponse = toEmbeddingsResponse(aiResponse, request);
                    log.info("向量化成功，tokens: {}, 向量数: {}",
                            embeddingsResponse.getUsage().getTotalTokens(),
                            embeddingsResponse.getData().size());
                    return Result.success(embeddingsResponse);
                })
                .onErrorResume(error -> {
                    java.time.LocalDateTime responseTime = java.time.LocalDateTime.now();
                    long duration = java.time.Duration.between(requestTime, responseTime).toMillis();

                    // 回滚配额
                    quotaManager.rollback(preDeductId);

                    // 发布失败日志事件
                    eventPublisher.publishFailureLogEvent(
                            requestId,
                            apiKey,
                            selectedModel,
                            "/api/v1/ai/embeddings",
                            RequestType.EMBEDDING,
                            request,
                            500,
                            error.getMessage(),
                            duration,
                            httpRequest.getRemoteAddr(),
                            httpRequest.getHeader("User-Agent")
                    );

                    log.error("向量化失败", error);
                    return Mono.just(Result.error(error.getMessage()));
                });
    }

    /**
     * 将EmbeddingsRequest转换为AiRequest
     * <p>
     * Embeddings请求不使用messages字段，而是使用input字段。
     * 这里我们临时将input作为模型字段传递，实际适配器会处理。
     * </p>
     */
    private AiRequest toAiRequest(EmbeddingsRequest request) {
        // 注意：对于embeddings，AiRequest的结构可能需要扩展
        // 目前我们使用model字段传递必要信息，实际实现中可能需要修改AiRequest
        return AiRequest.builder()
                .model(request.getModel() != null ? request.getModel() : request.getModelType())
                .stream(false) // Embeddings不支持流式
                .user(request.getUser())
                // 将input等embeddings特有参数通过自定义字段传递
                // 实际实现时可能需要扩展AiRequest类添加input、dimensions等字段
                .build();
    }

    /**
     * 将AiResponse转换为EmbeddingsResponse
     * <p>
     * 从AiResponse中提取向量数据，并根据encodingFormat转换为相应格式。
     * </p>
     */
    private EmbeddingsResponse toEmbeddingsResponse(AiResponse aiResponse, EmbeddingsRequest request) {
        // 注意：这是简化实现，实际需要从aiResponse中解析embedding数据
        // 真实场景中，AiResponse应该包含embeddings字段或在content中返回JSON

        List<Embedding> embeddings = parseEmbeddings(aiResponse, request);

        return EmbeddingsResponse.builder()
                .object("list")
                .data(embeddings)
                .model(aiResponse.getModel())
                .usage(Usage.builder()
                        .promptTokens(aiResponse.getPromptTokens())
                        .completionTokens(0) // Embeddings通常没有completion tokens
                        .totalTokens(aiResponse.calculateTotalTokens())
                        .build())
                .created(aiResponse.getCreated() != null ? aiResponse.getCreated() : Instant.now().getEpochSecond())
                .build();
    }

    /**
     * 从AiResponse中解析embeddings数据
     * <p>
     * 注意：这是临时实现。实际场景中需要：
     * 1. 扩展AiResponse添加embeddings字段
     * 2. 各个adapter返回标准化的embedding数据
     * 3. 根据encodingFormat进行转换
     * </p>
     */
    private List<Embedding> parseEmbeddings(AiResponse aiResponse, EmbeddingsRequest request) {
        List<Embedding> result = new ArrayList<>();

        // TODO: 实际实现需要从aiResponse中解析真实的embedding向量
        // 目前这是一个占位实现，返回空向量

        // 判断input是单个文本还是文本数组
        boolean isArray = request.getInput() instanceof List;
        int count = isArray ? ((List<?>) request.getInput()).size() : 1;

        for (int i = 0; i < count; i++) {
            if ("base64".equals(request.getEncodingFormat())) {
                // 返回Base64编码的向量
                result.add(Embedding.ofBase64(i, encodeToBase64(new ArrayList<>())));
            } else {
                // 返回浮点数向量（默认）
                result.add(Embedding.ofFloat(i, new ArrayList<>()));
            }
        }

        return result;
    }

    /**
     * 将浮点数向量编码为Base64字符串
     */
    private String encodeToBase64(List<Double> embedding) {
        // 将Double列表转换为字节数组，然后Base64编码
        // 注意：这是简化实现，实际需要按照IEEE 754标准转换
        byte[] bytes = new byte[embedding.size() * 8]; // 每个double 8字节

        for (int i = 0; i < embedding.size(); i++) {
            long bits = Double.doubleToLongBits(embedding.get(i));
            for (int j = 0; j < 8; j++) {
                bytes[i * 8 + j] = (byte) ((bits >> (8 * j)) & 0xFF);
            }
        }

        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 估算请求消耗的token数
     * <p>
     * 对于embeddings，token数主要由输入文本长度决定。
     * 简单估算：字符数 / 4
     * </p>
     */
    private int estimateTokens(EmbeddingsRequest request) {
        Object input = request.getInput();

        if (input instanceof String) {
            return ((String) input).length() / 4;
        } else if (input instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) input;
            return texts.stream()
                    .mapToInt(text -> text.length() / 4)
                    .sum();
        }

        return 100; // 默认值
    }
}
