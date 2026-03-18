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
import com.fsa.aicenter.infrastructure.adapter.common.AiStreamChunk;
import com.fsa.aicenter.infrastructure.event.DomainEventPublisher;
import com.fsa.aicenter.interfaces.api.dto.ChatRequest;
import com.fsa.aicenter.interfaces.api.dto.ChatResponse;
import com.fsa.aicenter.interfaces.api.dto.ChatStreamChunk;
import com.fsa.aicenter.interfaces.api.dto.Usage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Chat Completion API控制器
 * <p>
 * 提供AI聊天服务的核心API接口，支持流式和非流式响应。
 * </p>
 *
 * @author FSA AI Center
 */
@Tag(name = "AI聊天API", description = "Chat Completion接口")
@RestController
@RequestMapping("/api/v1/ai/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private AiProxyService aiProxyService;

    @Autowired
    private QuotaManager quotaManager;

    @Autowired
    private DomainEventPublisher eventPublisher;

    /**
     * Chat Completion（非流式）
     * <p>
     * 标准的聊天接口，返回完整的响应内容。
     * </p>
     *
     * @param chatRequest 聊天请求参数
     * @param httpRequest HTTP请求对象，从attribute获取已认证的ApiKey
     * @return 聊天响应结果
     */
    @Operation(summary = "非流式聊天", description = "返回完整的AI响应内容")
    @PostMapping(value = "/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Result<ChatResponse>> chatCompletions(
            @Valid @RequestBody ChatRequest chatRequest,
            HttpServletRequest httpRequest
    ) {
        log.info("收到非流式聊天请求，模型: {}, 类型: {}, 消息数: {}",
                chatRequest.getModel(), chatRequest.getModelType(), chatRequest.getMessages().size());

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
        int estimatedTokens = estimateTokens(chatRequest);
        String preDeductId = quotaManager.preDeduct(apiKey, estimatedTokens);

        // 转换为AiRequest
        AiRequest aiRequest = toAiRequest(chatRequest);

        // 调用AiProxyService并获取实际使用的模型
        Mono<AiResponse> responseMono;
        AiModel selectedModel; // 用于记录实际使用的模型
        if (chatRequest.getModel() != null && !chatRequest.getModel().isEmpty()) {
            // 指定模型，不降级
            selectedModel = aiProxyService.selectModelByCode(chatRequest.getModel());
            responseMono = aiProxyService.call(chatRequest.getModel(), aiRequest);
        } else {
            // 按类型选择，支持降级
            ModelType modelType = ModelType.fromCode(chatRequest.getModelType());
            List<AiModel> models = aiProxyService.selectModelsByType(modelType);
            selectedModel = models.get(0); // 主模型
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
                            "/api/v1/ai/chat/completions",
                            RequestType.CHAT,
                            false,
                            chatRequest,
                            aiResponse,
                            duration,
                            actualTokens,
                            httpRequest.getRemoteAddr(),
                            httpRequest.getHeader("User-Agent")
                    );

                    ChatResponse chatResponse = toChatResponse(aiResponse);
                    log.info("非流式聊天成功，ID: {}, tokens: {}",
                            chatResponse.getId(), chatResponse.getUsage().getTotalTokens());
                    return Result.success(chatResponse);
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
                            "/api/v1/ai/chat/completions",
                            RequestType.CHAT,
                            chatRequest,
                            500,
                            error.getMessage(),
                            duration,
                            httpRequest.getRemoteAddr(),
                            httpRequest.getHeader("User-Agent")
                    );

                    log.error("非流式聊天失败", error);
                    return Mono.just(Result.error(error.getMessage()));
                });
    }

    /**
     * Chat Completion（流式）
     * <p>
     * 流式聊天接口，通过SSE（Server-Sent Events）实时推送响应内容。
     * </p>
     *
     * @param chatRequest 聊天请求参数
     * @param httpRequest HTTP请求对象，从attribute获取已认证的ApiKey
     * @return 流式响应数据块
     */
    @Operation(summary = "流式聊天", description = "通过SSE实时推送AI响应")
    @PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamChunk>> chatCompletionsStream(
            @Valid @RequestBody ChatRequest chatRequest,
            HttpServletRequest httpRequest
    ) {
        log.info("收到流式聊天请求，模型: {}, 类型: {}, 消息数: {}",
                chatRequest.getModel(), chatRequest.getModelType(), chatRequest.getMessages().size());

        // 从request.getAttribute获取已认证的ApiKey
        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("apiKey");
        if (apiKey == null) {
            log.error("ApiKey未找到，认证过滤器可能未正确设置");
            return Flux.error(new IllegalStateException("认证失败"));
        }

        // 记录请求开始时间
        java.time.LocalDateTime requestTime = java.time.LocalDateTime.now();
        String requestId = java.util.UUID.randomUUID().toString();

        // 预扣配额
        int estimatedTokens = estimateTokens(chatRequest);
        String preDeductId = quotaManager.preDeduct(apiKey, estimatedTokens);

        // 转换为AiRequest
        AiRequest aiRequest = toAiRequest(chatRequest);
        aiRequest.setStream(true);

        // 调用AiProxyService并获取实际使用的模型
        Flux<AiStreamChunk> chunkFlux;
        AiModel selectedModel;
        if (chatRequest.getModel() != null && !chatRequest.getModel().isEmpty()) {
            // 指定模型，不降级
            selectedModel = aiProxyService.selectModelByCode(chatRequest.getModel());
            chunkFlux = aiProxyService.callStream(chatRequest.getModel(), aiRequest);
        } else {
            // 按类型选择，支持降级
            ModelType modelType = ModelType.fromCode(chatRequest.getModelType());
            List<AiModel> models = aiProxyService.selectModelsByType(modelType);
            selectedModel = models.get(0); // 主模型
            chunkFlux = aiProxyService.callStreamWithFallback(modelType, aiRequest);
        }

        // 用于累计token数
        AtomicInteger totalTokens = new AtomicInteger(0);
        AtomicReference<String> chunkId = new AtomicReference<>();

        return chunkFlux
                .map(aiChunk -> {
                    // 记录chunk ID（用于日志）
                    if (chunkId.get() == null && aiChunk.getId() != null) {
                        chunkId.set(aiChunk.getId());
                    }

                    // 累计token数（如果chunk中包含usage信息）
                    if (aiChunk.isDone() && aiChunk.getPromptTokens() != null) {
                        totalTokens.set(aiChunk.calculateTotalTokens());
                    }

                    ChatStreamChunk chatChunk = toChatStreamChunk(aiChunk);

                    // 构建SSE
                    ServerSentEvent.Builder<ChatStreamChunk> builder = ServerSentEvent.builder(chatChunk);
                    if (aiChunk.getId() != null) {
                        builder.id(aiChunk.getId());
                    }
                    builder.event("message");

                    return builder.build();
                })
                .doOnComplete(() -> {
                    java.time.LocalDateTime responseTime = java.time.LocalDateTime.now();
                    long duration = java.time.Duration.between(requestTime, responseTime).toMillis();

                    // 确认配额扣减
                    int actualTokens = totalTokens.get() > 0 ? totalTokens.get() : estimatedTokens;
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
                            "/api/v1/ai/chat/completions/stream",
                            RequestType.CHAT,
                            true,
                            chatRequest,
                            "Stream completed", // 流式响应不记录完整内容
                            duration,
                            actualTokens,
                            httpRequest.getRemoteAddr(),
                            httpRequest.getHeader("User-Agent")
                    );

                    log.info("流式聊天完成，ID: {}", requestId);
                })
                .doOnError(error -> {
                    java.time.LocalDateTime responseTime = java.time.LocalDateTime.now();
                    long duration = java.time.Duration.between(requestTime, responseTime).toMillis();

                    // 回滚配额
                    quotaManager.rollback(preDeductId);

                    // 发布失败日志事件
                    eventPublisher.publishFailureLogEvent(
                            requestId,
                            apiKey,
                            selectedModel,
                            "/api/v1/ai/chat/completions/stream",
                            RequestType.CHAT,
                            chatRequest,
                            500,
                            error.getMessage(),
                            duration,
                            httpRequest.getRemoteAddr(),
                            httpRequest.getHeader("User-Agent")
                    );

                    log.error("流式聊天失败", error);
                });
    }

    /**
     * 将ChatRequest转换为AiRequest
     */
    private AiRequest toAiRequest(ChatRequest chatRequest) {
        return AiRequest.builder()
                .model(chatRequest.getModel() != null ? chatRequest.getModel() : chatRequest.getModelType())
                .messages(chatRequest.getMessages())
                .stream(chatRequest.getStream())
                .temperature(chatRequest.getTemperature())
                .maxTokens(chatRequest.getMaxTokens())
                .topP(chatRequest.getTopP())
                .frequencyPenalty(chatRequest.getFrequencyPenalty())
                .presencePenalty(chatRequest.getPresencePenalty())
                .stop(chatRequest.getStop())
                .user(chatRequest.getUser())
                .build();
    }

    /**
     * 将AiResponse转换为ChatResponse
     */
    private ChatResponse toChatResponse(AiResponse aiResponse) {
        return ChatResponse.builder()
                .id(aiResponse.getId())
                .model(aiResponse.getModel())
                .content(aiResponse.getContent())
                .usage(Usage.builder()
                        .promptTokens(aiResponse.getPromptTokens())
                        .completionTokens(aiResponse.getCompletionTokens())
                        .totalTokens(aiResponse.calculateTotalTokens())
                        .build())
                .finishReason(aiResponse.getFinishReason())
                .created(aiResponse.getCreated())
                .build();
    }

    /**
     * 将AiStreamChunk转换为ChatStreamChunk
     */
    private ChatStreamChunk toChatStreamChunk(AiStreamChunk aiChunk) {
        ChatStreamChunk.ChatStreamChunkBuilder builder = ChatStreamChunk.builder()
                .id(aiChunk.getId())
                .model(aiChunk.getModel())
                .delta(aiChunk.getDelta())
                .done(aiChunk.isDone())
                .finishReason(aiChunk.getFinishReason())
                .created(aiChunk.getCreated());

        if (aiChunk.isDone() && aiChunk.getPromptTokens() != null) {
            builder.usage(Usage.builder()
                    .promptTokens(aiChunk.getPromptTokens())
                    .completionTokens(aiChunk.getCompletionTokens())
                    .totalTokens(aiChunk.calculateTotalTokens())
                    .build());
        }

        return builder.build();
    }

    /**
     * 估算请求消耗的token数
     * <p>
     * 简单估算：消息字符数 / 4（中文约3-4字符/token，英文约4字符/token）
     * </p>
     */
    private int estimateTokens(ChatRequest request) {
        return request.getMessages().stream()
                .mapToInt(m -> m.getContent().length() / 4)
                .sum();
    }
}
