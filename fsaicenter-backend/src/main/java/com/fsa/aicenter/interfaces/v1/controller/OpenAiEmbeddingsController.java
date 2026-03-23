package com.fsa.aicenter.interfaces.v1.controller;

import com.fsa.aicenter.application.service.AiProxyService;
import com.fsa.aicenter.application.service.QuotaManager;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.log.valueobject.RequestType;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.infrastructure.adapter.common.AiRequest;
import com.fsa.aicenter.infrastructure.adapter.common.AiResponse;
import com.fsa.aicenter.infrastructure.event.DomainEventPublisher;
import com.fsa.aicenter.interfaces.v1.dto.OpenAiEmbeddingsRequest;
import com.fsa.aicenter.interfaces.v1.dto.OpenAiEmbeddingsResponse;
import com.fsa.aicenter.interfaces.v1.dto.OpenAiUsage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * OpenAI 兼容 - Embeddings API
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAiEmbeddingsController {

    private final AiProxyService aiProxyService;
    private final QuotaManager quotaManager;
    private final DomainEventPublisher eventPublisher;

    @PostMapping(value = "/embeddings", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<OpenAiEmbeddingsResponse> createEmbeddings(
            @Valid @RequestBody OpenAiEmbeddingsRequest request,
            HttpServletRequest httpRequest
    ) {
        ApiKey apiKey = getApiKey(httpRequest);
        LocalDateTime requestTime = LocalDateTime.now();
        String requestId = UUID.randomUUID().toString();

        // 估算 token
        String inputText = extractInputText(request.getInput());
        int estimatedTokens = Math.max(inputText.length() / 4, 1);
        String preDeductId = quotaManager.preDeduct(apiKey, estimatedTokens);

        AiRequest aiRequest = AiRequest.builder()
                .model(request.getModel())
                .input(inputText)
                .encodingFormat(request.getEncodingFormat())
                .dimensions(request.getDimensions())
                .build();

        AiModel selectedModel = aiProxyService.selectModelByCode(request.getModel());

        return aiProxyService.call(request.getModel(), aiRequest)
                .map(aiResponse -> {
                    LocalDateTime responseTime = LocalDateTime.now();
                    long duration = Duration.between(requestTime, responseTime).toMillis();

                    int actualTokens = aiResponse.calculateTotalTokens();
                    quotaManager.confirm(preDeductId, actualTokens > 0 ? actualTokens : estimatedTokens);

                    eventPublisher.publishBillingEvent(requestId, apiKey, selectedModel, actualTokens, requestTime, responseTime);
                    eventPublisher.publishSuccessLogEvent(requestId, apiKey, selectedModel,
                            "/v1/embeddings", RequestType.CHAT, false,
                            request, aiResponse, duration, actualTokens,
                            aiResponse.getPromptTokens(), null,
                            httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

                    return toResponse(aiResponse, request.getModel());
                })
                .onErrorResume(error -> {
                    quotaManager.rollback(preDeductId);
                    log.error("Embeddings failed", error);
                    return Mono.error(new RuntimeException(error.getMessage()));
                });
    }

    private OpenAiEmbeddingsResponse toResponse(AiResponse resp, String model) {
        List<Double> embedding = resp.getEmbedding() != null ? resp.getEmbedding() : Collections.emptyList();

        return OpenAiEmbeddingsResponse.builder()
                .data(Collections.singletonList(
                        OpenAiEmbeddingsResponse.EmbeddingData.builder()
                                .embedding(embedding)
                                .index(0)
                                .build()
                ))
                .model(model)
                .usage(OpenAiUsage.builder()
                        .promptTokens(resp.getPromptTokens())
                        .totalTokens(resp.calculateTotalTokens())
                        .build())
                .build();
    }

    @SuppressWarnings("unchecked")
    private String extractInputText(Object input) {
        if (input instanceof String) {
            return (String) input;
        }
        if (input instanceof List) {
            List<String> list = (List<String>) input;
            return String.join(" ", list);
        }
        return input.toString();
    }

    private ApiKey getApiKey(HttpServletRequest request) {
        ApiKey apiKey = (ApiKey) request.getAttribute("apiKey");
        if (apiKey == null) {
            throw new IllegalStateException("Authentication failed");
        }
        return apiKey;
    }
}
