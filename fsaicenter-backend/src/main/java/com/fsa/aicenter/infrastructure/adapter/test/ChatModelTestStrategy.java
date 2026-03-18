package com.fsa.aicenter.infrastructure.adapter.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsa.aicenter.application.dto.request.TestModelRequest;
import com.fsa.aicenter.application.dto.response.TestModelResponse;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import com.fsa.aicenter.infrastructure.adapter.AdapterFactory;
import com.fsa.aicenter.infrastructure.adapter.common.AiProviderAdapter;
import com.fsa.aicenter.infrastructure.adapter.common.AiRequest;
import com.fsa.aicenter.infrastructure.adapter.common.AiResponse;
import com.fsa.aicenter.infrastructure.adapter.common.AiStreamChunk;
import com.fsa.aicenter.infrastructure.adapter.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 对话模型测试策略
 *
 * @author FSA AI Center
 */
@Component
public class ChatModelTestStrategy implements ModelTestStrategy {

    private static final Logger log = LoggerFactory.getLogger(ChatModelTestStrategy.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AdapterFactory adapterFactory;

    public ChatModelTestStrategy(AdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @Override
    public SseEmitter testStream(AiModel model, Provider provider, TestModelRequest request) {
        SseEmitter emitter = new SseEmitter(120000L); // 2分钟超时
        long startTime = System.currentTimeMillis();

        // 构建AI请求
        AiRequest aiRequest = AiRequest.builder()
                .model(model.getCode())
                .messages(Collections.singletonList(Message.user(request.getText())))
                .stream(true)
                .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 500)
                .build();

        // 获取适配器
        AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());

        // 用于累计token
        AtomicInteger inputTokens = new AtomicInteger(0);
        AtomicInteger outputTokens = new AtomicInteger(0);
        AtomicLong firstChunkTime = new AtomicLong(0);

        // 订阅流式响应
        adapter.callStream(model, aiRequest)
                .subscribe(
                        chunk -> {
                            try {
                                // 记录首字时间
                                if (firstChunkTime.get() == 0 && chunk.hasContent()) {
                                    firstChunkTime.set(System.currentTimeMillis() - startTime);
                                }

                                // 发送内容块
                                if (chunk.hasContent()) {
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("type", "content");
                                    data.put("content", chunk.getDelta());
                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(objectMapper.writeValueAsString(data)));
                                }

                                // 记录token
                                if (chunk.isDone()) {
                                    if (chunk.getPromptTokens() != null) {
                                        inputTokens.set(chunk.getPromptTokens());
                                    }
                                    if (chunk.getCompletionTokens() != null) {
                                        outputTokens.set(chunk.getCompletionTokens());
                                    }
                                }
                            } catch (IOException e) {
                                log.error("发送SSE消息失败", e);
                                // 连接已断开，直接完成
                                emitter.complete();
                            }
                        },
                        error -> {
                            try {
                                log.error("流式测试发生错误", error);
                                Map<String, Object> data = new HashMap<>();
                                data.put("type", "error");
                                data.put("error", error.getMessage());
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(objectMapper.writeValueAsString(data)));
                                // 错误已通过SSE事件发送给客户端，正常完成连接
                                // 不要使用 completeWithError，否则会触发全局异常处理器
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("发送错误消息失败", e);
                                emitter.complete();
                            }
                        },
                        () -> {
                            try {
                                long duration = System.currentTimeMillis() - startTime;
                                Map<String, Object> data = new HashMap<>();
                                data.put("type", "done");
                                data.put("duration", duration);
                                data.put("firstChunkTime", firstChunkTime.get());
                                data.put("inputTokens", inputTokens.get());
                                data.put("outputTokens", outputTokens.get());
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(objectMapper.writeValueAsString(data)));
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("发送完成消息失败", e);
                                // 连接已断开，直接完成
                                emitter.complete();
                            }
                        }
                );

        emitter.onTimeout(() -> {
            log.warn("流式测试超时");
            emitter.complete();
        });

        emitter.onCompletion(() -> log.debug("流式测试完成"));

        return emitter;
    }

    @Override
    public ModelType getSupportedType() {
        return ModelType.CHAT;
    }

    @Override
    public TestModelResponse test(AiModel model, Provider provider, TestModelRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 决定是否使用流式
            boolean useStream = request.getStream() != null
                    ? request.getStream()
                    : model.getSupportStream();

            // 构建AI请求
            AiRequest aiRequest = AiRequest.builder()
                    .model(model.getCode())
                    .messages(Collections.singletonList(Message.user(request.getText())))
                    .stream(useStream)
                    .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                    .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 500)
                    .build();

            // 获取适配器并调用
            AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());
            AiResponse aiResponse = adapter.call(model, aiRequest).block();

            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(true)
                    .duration(duration)
                    .content(aiResponse.getContent())
                    .inputTokens(aiResponse.getPromptTokens())
                    .outputTokens(aiResponse.getCompletionTokens())
                    .build();

        } catch (Exception e) {
            log.error("Chat model test failed: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .duration(duration)
                    .build();
        }
    }
}
