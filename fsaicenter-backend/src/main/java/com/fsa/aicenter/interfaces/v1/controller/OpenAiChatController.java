package com.fsa.aicenter.interfaces.v1.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsa.aicenter.application.service.AiProxyService;
import com.fsa.aicenter.application.service.QuotaManager;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.log.valueobject.RequestType;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.infrastructure.adapter.common.AiRequest;
import com.fsa.aicenter.infrastructure.adapter.common.AiResponse;
import com.fsa.aicenter.infrastructure.adapter.common.AiStreamChunk;
import com.fsa.aicenter.infrastructure.event.DomainEventPublisher;
import com.fsa.aicenter.interfaces.v1.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OpenAI 兼容 - Chat Completions API
 * <p>
 * 透传客户端原始请求体给上游API，确保tools、tool_choice等参数不丢失。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class OpenAiChatController {

    private final AiProxyService aiProxyService;
    private final QuotaManager quotaManager;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/completions")
    public void chatCompletions(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) throws IOException {
        String rawBody = new String(httpRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        OpenAiChatRequest chatRequest = objectMapper.readValue(rawBody, OpenAiChatRequest.class);

        if (chatRequest.getModel() == null || chatRequest.getModel().isBlank()) {
            httpResponse.setStatus(400);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(httpResponse.getOutputStream(),
                    OpenAiErrorResponse.of("model is required", "invalid_request_error", "invalid_request"));
            return;
        }
        if (chatRequest.getMessages() == null || chatRequest.getMessages().isEmpty()) {
            httpResponse.setStatus(400);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(httpResponse.getOutputStream(),
                    OpenAiErrorResponse.of("messages is required", "invalid_request_error", "invalid_request"));
            return;
        }

        if (chatRequest.isStream()) {
            streamChat(chatRequest, rawBody, httpRequest, httpResponse);
        } else {
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setCharacterEncoding("UTF-8");
            nonStreamChat(chatRequest, rawBody, httpRequest, httpResponse);
        }
    }

    private void nonStreamChat(
            OpenAiChatRequest chatRequest, String rawBody,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        ApiKey apiKey = getApiKey(httpRequest);
        LocalDateTime requestTime = LocalDateTime.now();
        String requestId = UUID.randomUUID().toString();

        int estimatedTokens = estimateTokens(chatRequest);
        String preDeductId = quotaManager.preDeduct(apiKey, estimatedTokens);

        AiRequest aiRequest = toAiRequest(chatRequest, rawBody);
        AiModel selectedModel = aiProxyService.selectModelByCode(chatRequest.getModel());

        try {
            AiResponse aiResponse = aiProxyService.call(chatRequest.getModel(), aiRequest).block();
            LocalDateTime responseTime = LocalDateTime.now();
            long duration = Duration.between(requestTime, responseTime).toMillis();

            int actualTokens = aiResponse.calculateTotalTokens();
            quotaManager.confirm(preDeductId, actualTokens);

            eventPublisher.publishBillingEvent(requestId, apiKey, selectedModel, actualTokens, requestTime, responseTime);
            eventPublisher.publishSuccessLogEvent(requestId, apiKey, selectedModel,
                    "/v1/chat/completions", RequestType.CHAT, false,
                    chatRequest, aiResponse, duration, actualTokens,
                    aiResponse.getPromptTokens(), aiResponse.getCompletionTokens(),
                    httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

            // 优先透传上游原始响应（保留tool_calls等所有字段）
            if (aiResponse.getRawResponseBody() != null) {
                httpResponse.getWriter().write(aiResponse.getRawResponseBody());
                httpResponse.getWriter().flush();
            } else {
                objectMapper.writeValue(httpResponse.getOutputStream(), toOpenAiResponse(aiResponse));
            }
        } catch (Exception error) {
            LocalDateTime responseTime = LocalDateTime.now();
            long duration = Duration.between(requestTime, responseTime).toMillis();
            quotaManager.rollback(preDeductId);

            eventPublisher.publishFailureLogEvent(requestId, apiKey, selectedModel,
                    "/v1/chat/completions", RequestType.CHAT, chatRequest,
                    500, error.getMessage(), duration,
                    httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

            log.error("Chat completion failed", error);
            throw new RuntimeException(error.getMessage());
        }
    }

    private void streamChat(OpenAiChatRequest chatRequest, String rawBody,
                            HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        httpResponse.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setBufferSize(0);
        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setHeader("Connection", "keep-alive");
        httpResponse.setHeader("X-Accel-Buffering", "no");

        ApiKey apiKey = getApiKey(httpRequest);
        LocalDateTime requestTime = LocalDateTime.now();
        String requestId = UUID.randomUUID().toString();

        int estimatedTokens = estimateTokens(chatRequest);
        String preDeductId = quotaManager.preDeduct(apiKey, estimatedTokens);

        AiRequest aiRequest = toAiRequest(chatRequest, rawBody);
        aiRequest.setStream(true);

        AiModel selectedModel = aiProxyService.selectModelByCode(chatRequest.getModel());

        PrintWriter writer = httpResponse.getWriter();
        AtomicInteger totalTokens = new AtomicInteger(0);
        AtomicInteger streamPromptTokens = new AtomicInteger(0);
        AtomicInteger streamCompletionTokens = new AtomicInteger(0);

        try {
            aiProxyService.callStream(chatRequest.getModel(), aiRequest)
                    .toStream()
                    .forEach(aiChunk -> {
                        if (aiChunk.isDone() && aiChunk.getPromptTokens() != null) {
                            totalTokens.set(aiChunk.calculateTotalTokens());
                            streamPromptTokens.set(aiChunk.getPromptTokens());
                            if (aiChunk.getCompletionTokens() != null) {
                                streamCompletionTokens.set(aiChunk.getCompletionTokens());
                            }
                        }
                        try {
                            // 优先透传上游原始chunk数据（保留tool_calls等所有字段）
                            if (aiChunk.getRawData() != null) {
                                writer.write("data: " + aiChunk.getRawData() + "\n\n");
                            } else {
                                OpenAiChatStreamChunk chunk = toStreamChunk(aiChunk, chatRequest.getModel());
                                writer.write("data: " + objectMapper.writeValueAsString(chunk) + "\n\n");
                            }
                            writer.flush();
                        } catch (JsonProcessingException e) {
                            log.warn("序列化流式chunk失败", e);
                        }
                    });

            writer.write("data: [DONE]\n\n");
            writer.flush();

            LocalDateTime responseTime = LocalDateTime.now();
            long duration = Duration.between(requestTime, responseTime).toMillis();
            int actualTokens = totalTokens.get() > 0 ? totalTokens.get() : estimatedTokens;
            quotaManager.confirm(preDeductId, actualTokens);

            eventPublisher.publishBillingEvent(requestId, apiKey, selectedModel, actualTokens, requestTime, responseTime);
            eventPublisher.publishSuccessLogEvent(requestId, apiKey, selectedModel,
                    "/v1/chat/completions", RequestType.CHAT, true,
                    chatRequest, "Stream completed", duration, actualTokens,
                    streamPromptTokens.get() > 0 ? streamPromptTokens.get() : null,
                    streamCompletionTokens.get() > 0 ? streamCompletionTokens.get() : null,
                    httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

        } catch (Exception error) {
            LocalDateTime responseTime = LocalDateTime.now();
            long duration = Duration.between(requestTime, responseTime).toMillis();
            quotaManager.rollback(preDeductId);

            eventPublisher.publishFailureLogEvent(requestId, apiKey, selectedModel,
                    "/v1/chat/completions", RequestType.CHAT, chatRequest,
                    500, error.getMessage(), duration,
                    httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

            log.error("Stream chat failed", error);

            if (!httpResponse.isCommitted()) {
                try {
                    writer.write("data: " + objectMapper.writeValueAsString(
                            OpenAiErrorResponse.of(error.getMessage(), "server_error", "internal_error")) + "\n\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } catch (Exception e) {
                    log.warn("写入错误响应失败", e);
                }
            }
        }
    }

    private ApiKey getApiKey(HttpServletRequest request) {
        ApiKey apiKey = (ApiKey) request.getAttribute("apiKey");
        if (apiKey == null) {
            throw new IllegalStateException("Authentication failed");
        }
        return apiKey;
    }

    private AiRequest toAiRequest(OpenAiChatRequest req, String rawBody) {
        return AiRequest.builder()
                .model(req.getModel())
                .messages(req.getMessages())
                .stream(req.isStream())
                .temperature(req.getTemperature())
                .maxTokens(req.getMaxCompletionTokens() != null ? req.getMaxCompletionTokens() : req.getMaxTokens())
                .topP(req.getTopP())
                .frequencyPenalty(req.getFrequencyPenalty())
                .presencePenalty(req.getPresencePenalty())
                .stop(req.getStop())
                .user(req.getUser())
                .rawRequestBody(rawBody)
                .build();
    }

    private OpenAiChatResponse toOpenAiResponse(AiResponse resp) {
        return OpenAiChatResponse.builder()
                .id(resp.getId() != null ? resp.getId() : "chatcmpl-" + UUID.randomUUID().toString().substring(0, 12))
                .created(resp.getCreated() != null ? resp.getCreated() : System.currentTimeMillis() / 1000)
                .model(resp.getModel())
                .choices(Collections.singletonList(
                        OpenAiChatResponse.Choice.builder()
                                .index(0)
                                .message(OpenAiChatResponse.ChatMessage.builder()
                                        .role("assistant")
                                        .content(resp.getContent())
                                        .build())
                                .finishReason(resp.getFinishReason() != null ? resp.getFinishReason() : "stop")
                                .build()
                ))
                .usage(OpenAiUsage.builder()
                        .promptTokens(resp.getPromptTokens())
                        .completionTokens(resp.getCompletionTokens())
                        .totalTokens(resp.calculateTotalTokens())
                        .build())
                .build();
    }

    private OpenAiChatStreamChunk toStreamChunk(AiStreamChunk chunk, String model) {
        OpenAiChatStreamChunk.Delta delta = OpenAiChatStreamChunk.Delta.builder()
                .content(chunk.getDelta())
                .build();

        String finishReason = chunk.isDone()
                ? (chunk.getFinishReason() != null ? chunk.getFinishReason() : "stop")
                : null;

        OpenAiChatStreamChunk.OpenAiChatStreamChunkBuilder builder = OpenAiChatStreamChunk.builder()
                .id(chunk.getId() != null ? chunk.getId() : "chatcmpl-stream")
                .created(chunk.getCreated() != null ? chunk.getCreated() : System.currentTimeMillis() / 1000)
                .model(chunk.getModel() != null ? chunk.getModel() : model)
                .choices(Collections.singletonList(
                        OpenAiChatStreamChunk.StreamChoice.builder()
                                .index(0)
                                .delta(delta)
                                .finishReason(finishReason)
                                .build()
                ));

        if (chunk.isDone() && chunk.getPromptTokens() != null) {
            builder.usage(OpenAiUsage.builder()
                    .promptTokens(chunk.getPromptTokens())
                    .completionTokens(chunk.getCompletionTokens())
                    .totalTokens(chunk.calculateTotalTokens())
                    .build());
        }

        return builder.build();
    }

    private int estimateTokens(OpenAiChatRequest request) {
        int estimated = request.getMessages().stream()
                .mapToInt(m -> m.getContent() != null ? m.getContent().length() / 4 : 0)
                .sum();
        return Math.max(estimated, 1);
    }
}
