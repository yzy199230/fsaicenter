package com.fsa.aicenter.infrastructure.adapter.test;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 语音识别（ASR）模型测试策略
 *
 * @author FSA AI Center
 */
@Component
public class AsrModelTestStrategy implements ModelTestStrategy {

    private static final Logger log = LoggerFactory.getLogger(AsrModelTestStrategy.class);

    private final AdapterFactory adapterFactory;

    public AsrModelTestStrategy(AdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ModelType getSupportedType() {
        return ModelType.ASR;
    }

    @Override
    public TestModelResponse test(AiModel model, Provider provider, TestModelRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 验证音频输入
            if (request.getAudio() == null || request.getAudio().isBlank()) {
                return TestModelResponse.builder()
                        .success(false)
                        .errorMessage("语音识别需要提供音频文件")
                        .duration(0L)
                        .build();
            }

            // 构建AI请求
            AiRequest aiRequest = AiRequest.builder()
                    .model(model.getCode())
                    .audio(request.getAudio())
                    .audioFormat(request.getAudioFormat())
                    .build();

            // 获取适配器并调用
            AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());
            AiResponse aiResponse = adapter.speechToText(model, aiRequest).block();

            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(true)
                    .duration(duration)
                    .content(aiResponse.getContent())
                    .build();

        } catch (Exception e) {
            log.error("ASR model test failed: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .duration(duration)
                    .build();
        }
    }

    @Override
    public SseEmitter testStream(AiModel model, Provider provider, TestModelRequest request) {
        SseEmitter emitter = new SseEmitter(120000L); // 2分钟超时
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

        try {
            // 验证音频输入
            if (request.getAudio() == null || request.getAudio().isBlank()) {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"语音识别需要提供音频文件\"}"));
                emitter.complete();
                return emitter;
            }

            // 构建AI请求
            AiRequest aiRequest = AiRequest.builder()
                    .model(model.getCode())
                    .audio(request.getAudio())
                    .audioFormat(request.getAudioFormat())
                    .build();

            // 获取适配器
            AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());

            // 流式调用
            adapter.speechToTextStream(model, aiRequest)
                    .subscribe(
                            chunk -> {
                                try {
                                    if (chunk.isDone()) {
                                        // 发送完成事件
                                        long duration = System.currentTimeMillis() - startTime.get();
                                        String doneData = String.format(
                                                "{\"done\":true,\"duration\":%d}", duration);
                                        emitter.send(SseEmitter.event()
                                                .name("done")
                                                .data(doneData));
                                        emitter.complete();
                                    } else {
                                        // 发送识别文本
                                        String text = chunk.getDelta();
                                        if (text != null && !text.isEmpty()) {
                                            String data = String.format("{\"text\":\"%s\"}",
                                                    escapeJson(text));
                                            emitter.send(SseEmitter.event()
                                                    .name("message")
                                                    .data(data));
                                        }
                                    }
                                } catch (IOException e) {
                                    log.error("发送SSE事件失败", e);
                                    emitter.completeWithError(e);
                                }
                            },
                            error -> {
                                log.error("ASR流式测试失败", error);
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data("{\"error\":\"" + escapeJson(error.getMessage()) + "\"}"));
                                } catch (IOException e) {
                                    log.error("发送错误事件失败", e);
                                }
                                emitter.completeWithError(error);
                            },
                            () -> {
                                // 如果没有通过done chunk完成，这里兜底
                                try {
                                    if (!emitter.toString().contains("complete")) {
                                        long duration = System.currentTimeMillis() - startTime.get();
                                        String doneData = String.format(
                                                "{\"done\":true,\"duration\":%d}", duration);
                                        emitter.send(SseEmitter.event()
                                                .name("done")
                                                .data(doneData));
                                        emitter.complete();
                                    }
                                } catch (Exception e) {
                                    // 忽略
                                }
                            }
                    );

        } catch (Exception e) {
            log.error("ASR流式测试异常", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"));
            } catch (IOException ex) {
                log.error("发送错误事件失败", ex);
            }
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
