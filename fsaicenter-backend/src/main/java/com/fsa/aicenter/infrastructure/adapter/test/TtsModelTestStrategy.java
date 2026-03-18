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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 语音合成（TTS）模型测试策略
 *
 * @author FSA AI Center
 */
@Component
public class TtsModelTestStrategy implements ModelTestStrategy {

    private static final Logger log = LoggerFactory.getLogger(TtsModelTestStrategy.class);

    private final AdapterFactory adapterFactory;

    public TtsModelTestStrategy(AdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ModelType getSupportedType() {
        return ModelType.TTS;
    }

    @Override
    public TestModelResponse test(AiModel model, Provider provider, TestModelRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 验证文本输入
            if (request.getText() == null || request.getText().isBlank()) {
                return TestModelResponse.builder()
                        .success(false)
                        .errorMessage("语音合成需要提供文本内容")
                        .duration(0L)
                        .build();
            }

            // 构建AI请求
            AiRequest.AiRequestBuilder builder = AiRequest.builder()
                    .model(model.getCode())
                    .input(request.getText());

            // 设置可选参数
            if (request.getVoice() != null) {
                builder.voice(request.getVoice());
            }
            if (request.getSpeed() != null) {
                builder.speed(request.getSpeed());
            }

            AiRequest aiRequest = builder.build();

            // 获取适配器并调用
            AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());
            AiResponse aiResponse = adapter.textToSpeech(model, aiRequest).block();

            long duration = System.currentTimeMillis() - startTime;

            // 优先使用audioUrl，如果没有则将audioBase64转换为Data URL
            String audioUrl = aiResponse.getAudioUrl();
            if (audioUrl == null && aiResponse.getAudioBase64() != null) {
                audioUrl = "data:audio/mp3;base64," + aiResponse.getAudioBase64();
            }

            return TestModelResponse.builder()
                    .success(true)
                    .duration(duration)
                    .audioUrl(audioUrl)
                    .build();

        } catch (Exception e) {
            log.error("TTS model test failed: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .duration(duration)
                    .build();
        }
    }
}
