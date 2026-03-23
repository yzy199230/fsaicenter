package com.fsa.aicenter.interfaces.v1.controller;

import com.fsa.aicenter.application.service.AiProxyService;
import com.fsa.aicenter.application.service.QuotaManager;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.log.valueobject.RequestType;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.infrastructure.adapter.common.AiRequest;
import com.fsa.aicenter.infrastructure.adapter.common.AiResponse;
import com.fsa.aicenter.infrastructure.event.DomainEventPublisher;
import com.fsa.aicenter.interfaces.v1.dto.OpenAiImageRequest;
import com.fsa.aicenter.interfaces.v1.dto.OpenAiImageResponse;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OpenAI 兼容 - Images API
 */
@Slf4j
@RestController
@RequestMapping("/v1/images")
@RequiredArgsConstructor
public class OpenAiImagesController {

    private final AiProxyService aiProxyService;
    private final QuotaManager quotaManager;
    private final DomainEventPublisher eventPublisher;

    @PostMapping(value = "/generations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<OpenAiImageResponse> createImage(
            @Valid @RequestBody OpenAiImageRequest request,
            HttpServletRequest httpRequest
    ) {
        ApiKey apiKey = getApiKey(httpRequest);
        LocalDateTime requestTime = LocalDateTime.now();
        String requestId = UUID.randomUUID().toString();

        // 图片按固定费用计算配额
        int imageCost = calculateImageCost(request);
        String preDeductId = quotaManager.preDeduct(apiKey, imageCost);

        AiRequest aiRequest = AiRequest.builder()
                .model(request.getModel())
                .prompt(request.getPrompt())
                .n(request.getN())
                .size(request.getSize())
                .quality(request.getQuality())
                .style(request.getStyle())
                .responseFormat(request.getResponseFormat())
                .user(request.getUser())
                .build();

        AiModel selectedModel = aiProxyService.selectModelByCode(request.getModel());

        return aiProxyService.call(request.getModel(), aiRequest)
                .map(aiResponse -> {
                    LocalDateTime responseTime = LocalDateTime.now();
                    long duration = Duration.between(requestTime, responseTime).toMillis();
                    quotaManager.confirm(preDeductId, imageCost);

                    eventPublisher.publishBillingEvent(requestId, apiKey, selectedModel, imageCost, requestTime, responseTime);
                    eventPublisher.publishSuccessLogEvent(requestId, apiKey, selectedModel,
                            "/v1/images/generations", RequestType.CHAT, false,
                            request, aiResponse, duration, imageCost,
                            null, null,
                            httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

                    return toResponse(aiResponse);
                })
                .onErrorResume(error -> {
                    quotaManager.rollback(preDeductId);
                    log.error("Image generation failed", error);
                    return Mono.error(new RuntimeException(error.getMessage()));
                });
    }

    private OpenAiImageResponse toResponse(AiResponse resp) {
        if (resp.getImages() != null && !resp.getImages().isEmpty()) {
            return OpenAiImageResponse.builder()
                    .created(System.currentTimeMillis() / 1000)
                    .data(resp.getImages().stream()
                            .map(img -> OpenAiImageResponse.ImageData.builder()
                                    .url(img.getUrl())
                                    .b64Json(img.getB64Json())
                                    .revisedPrompt(img.getRevisedPrompt())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        }

        // Fallback: use imageUrls
        if (resp.getImageUrls() != null && !resp.getImageUrls().isEmpty()) {
            return OpenAiImageResponse.builder()
                    .created(System.currentTimeMillis() / 1000)
                    .data(resp.getImageUrls().stream()
                            .map(url -> OpenAiImageResponse.ImageData.builder().url(url).build())
                            .collect(Collectors.toList()))
                    .build();
        }

        return OpenAiImageResponse.builder()
                .created(System.currentTimeMillis() / 1000)
                .data(Collections.emptyList())
                .build();
    }

    private int calculateImageCost(OpenAiImageRequest request) {
        int baseCost = 1000; // 基础 token 成本
        int n = request.getN() != null ? request.getN() : 1;

        // 按尺寸调整
        if ("1024x1024".equals(request.getSize())) baseCost = 2000;
        else if ("1024x1792".equals(request.getSize()) || "1792x1024".equals(request.getSize())) baseCost = 3000;

        // HD 质量加倍
        if ("hd".equalsIgnoreCase(request.getQuality())) baseCost *= 2;

        return baseCost * n;
    }

    private ApiKey getApiKey(HttpServletRequest request) {
        ApiKey apiKey = (ApiKey) request.getAttribute("apiKey");
        if (apiKey == null) {
            throw new IllegalStateException("Authentication failed");
        }
        return apiKey;
    }
}
