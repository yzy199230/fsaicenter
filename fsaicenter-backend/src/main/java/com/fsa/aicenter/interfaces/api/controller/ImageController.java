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
import com.fsa.aicenter.interfaces.api.dto.ImageData;
import com.fsa.aicenter.interfaces.api.dto.ImageRequest;
import com.fsa.aicenter.interfaces.api.dto.ImageResponse;
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

import java.util.stream.Collectors;

/**
 * Image Generation API控制器
 * <p>
 * 提供AI图片生成服务的API接口。
 * </p>
 *
 * @author FSA AI Center
 */
@Tag(name = "AI图片生成API", description = "Image Generation接口")
@RestController
@RequestMapping("/api/v1/ai/images")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private AiProxyService aiProxyService;

    @Autowired
    private QuotaManager quotaManager;

    @Autowired
    private DomainEventPublisher eventPublisher;

    /**
     * Image Generation
     * <p>
     * 根据文本描述生成图片。
     * </p>
     *
     * @param imageRequest 图片生成请求参数
     * @param httpRequest  HTTP请求对象，从attribute获取已认证的ApiKey
     * @return 图片生成响应结果
     */
    @Operation(summary = "图片生成", description = "根据文本描述生成图片")
    @PostMapping(value = "/generations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Result<ImageResponse>> generateImages(
            @Valid @RequestBody ImageRequest imageRequest,
            HttpServletRequest httpRequest
    ) {
        log.info("收到图片生成请求，模型: {}, 类型: {}, 提示词: {}, 数量: {}",
                imageRequest.getModel(), imageRequest.getModelType(),
                imageRequest.getPrompt(), imageRequest.getN());

        // 从request.getAttribute获取已认证的ApiKey
        ApiKey apiKey = (ApiKey) httpRequest.getAttribute("apiKey");
        if (apiKey == null) {
            log.error("ApiKey未找到，认证过滤器可能未正确设置");
            return Mono.just(Result.error("认证失败"));
        }

        // 记录请求开始时间
        java.time.LocalDateTime requestTime = java.time.LocalDateTime.now();
        String requestId = java.util.UUID.randomUUID().toString();

        // 预扣配额（图片计费按数量和尺寸）
        int estimatedCost = estimateImageCost(imageRequest);
        String preDeductId = quotaManager.preDeduct(apiKey, estimatedCost);

        // 转换为AiRequest
        AiRequest aiRequest = toAiRequest(imageRequest);

        // 调用AiProxyService并获取实际使用的模型
        Mono<AiResponse> responseMono;
        AiModel selectedModel;
        if (imageRequest.getModel() != null && !imageRequest.getModel().isEmpty()) {
            // 指定模型，不降级
            selectedModel = aiProxyService.selectModelByCode(imageRequest.getModel());
            responseMono = aiProxyService.call(imageRequest.getModel(), aiRequest);
        } else {
            // 按类型选择，支持降级
            ModelType modelType = ModelType.fromCode(imageRequest.getModelType());
            java.util.List<AiModel> models = aiProxyService.selectModelsByType(modelType);
            selectedModel = models.get(0);
            responseMono = aiProxyService.callWithFallback(modelType, aiRequest);
        }

        return responseMono
                .map(aiResponse -> {
                    java.time.LocalDateTime responseTime = java.time.LocalDateTime.now();
                    long duration = java.time.Duration.between(requestTime, responseTime).toMillis();

                    // 确认配额扣减（图片实际生成数量可能与请求不同）
                    int actualCost = calculateImageCost(aiResponse, imageRequest);
                    quotaManager.confirm(preDeductId, actualCost);

                    // 发布计费事件（图片使用cost而非tokens）
                    eventPublisher.publishBillingEvent(
                            requestId,
                            apiKey,
                            selectedModel,
                            null, // tokens为null
                            actualCost, // 使用cost
                            requestTime,
                            responseTime
                    );

                    // 发布日志事件
                    eventPublisher.publishSuccessLogEvent(
                            requestId,
                            apiKey,
                            selectedModel,
                            "/api/v1/ai/images/generations",
                            RequestType.IMAGE,
                            false,
                            imageRequest,
                            aiResponse,
                            duration,
                            null, // 图片生成不计算tokens
                            httpRequest.getRemoteAddr(),
                            httpRequest.getHeader("User-Agent")
                    );

                    ImageResponse imageResponse = toImageResponse(aiResponse);
                    log.info("图片生成成功，ID: {}, 图片数量: {}",
                            imageResponse.getId(), imageResponse.getData().size());
                    return Result.success(imageResponse);
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
                            "/api/v1/ai/images/generations",
                            RequestType.IMAGE,
                            imageRequest,
                            500,
                            error.getMessage(),
                            duration,
                            httpRequest.getRemoteAddr(),
                            httpRequest.getHeader("User-Agent")
                    );

                    log.error("图片生成失败", error);
                    return Mono.just(Result.error(error.getMessage()));
                });
    }

    /**
     * 将ImageRequest转换为AiRequest
     */
    private AiRequest toAiRequest(ImageRequest imageRequest) {
        return AiRequest.builder()
                .model(imageRequest.getModel() != null ? imageRequest.getModel() : imageRequest.getModelType())
                .prompt(imageRequest.getPrompt())
                .negativePrompt(imageRequest.getNegativePrompt())
                .n(imageRequest.getN())
                .size(imageRequest.getSize())
                .responseFormat(imageRequest.getResponseFormat())
                .style(imageRequest.getStyle())
                .quality(imageRequest.getQuality())
                .user(imageRequest.getUser())
                .build();
    }

    /**
     * 将AiResponse转换为ImageResponse
     */
    private ImageResponse toImageResponse(AiResponse aiResponse) {
        return ImageResponse.builder()
                .id(aiResponse.getId())
                .model(aiResponse.getModel())
                .created(aiResponse.getCreated())
                .data(aiResponse.getImages().stream()
                        .map(this::toImageData)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 将AiResponse.ImageData转换为ImageData
     */
    private ImageData toImageData(AiResponse.ImageData aiImageData) {
        return ImageData.builder()
                .url(aiImageData.getUrl())
                .b64Json(aiImageData.getB64Json())
                .revisedPrompt(aiImageData.getRevisedPrompt())
                .build();
    }

    /**
     * 估算图片生成的成本（用于预扣配额）
     * <p>
     * 简单估算：
     * - 256x256: 1 credit
     * - 512x512: 2 credits
     * - 1024x1024: 4 credits
     * - 1024x1792/1792x1024: 8 credits
     * - HD质量 x2
     * </p>
     */
    private int estimateImageCost(ImageRequest request) {
        int baseCost = 4; // 默认1024x1024

        if (request.getSize() != null) {
            switch (request.getSize()) {
                case "256x256":
                    baseCost = 1;
                    break;
                case "512x512":
                    baseCost = 2;
                    break;
                case "1024x1024":
                    baseCost = 4;
                    break;
                case "1024x1792":
                case "1792x1024":
                    baseCost = 8;
                    break;
            }
        }

        // HD质量翻倍
        if ("hd".equals(request.getQuality())) {
            baseCost *= 2;
        }

        // 乘以生成数量
        return baseCost * request.getN();
    }

    /**
     * 计算图片实际生成的成本（用于确认配额）
     * <p>
     * 根据实际生成的图片���量和请求参数计算。
     * </p>
     */
    private int calculateImageCost(AiResponse aiResponse, ImageRequest request) {
        // 获取实际生成的图片数量
        int actualCount = aiResponse.getImages() != null ? aiResponse.getImages().size() : 0;

        if (actualCount == 0) {
            return 0;
        }

        int baseCost = 4; // 默认1024x1024

        if (request.getSize() != null) {
            switch (request.getSize()) {
                case "256x256":
                    baseCost = 1;
                    break;
                case "512x512":
                    baseCost = 2;
                    break;
                case "1024x1024":
                    baseCost = 4;
                    break;
                case "1024x1792":
                case "1792x1024":
                    baseCost = 8;
                    break;
            }
        }

        // HD质量翻倍
        if ("hd".equals(request.getQuality())) {
            baseCost *= 2;
        }

        // 乘以实际生成数量
        return baseCost * actualCount;
    }
}
