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
import com.fsa.aicenter.infrastructure.adapter.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 图像识别模型测试策略
 *
 * @author FSA AI Center
 */
@Component
public class ImageRecognitionModelTestStrategy implements ModelTestStrategy {

    private static final Logger log = LoggerFactory.getLogger(ImageRecognitionModelTestStrategy.class);

    private final AdapterFactory adapterFactory;

    public ImageRecognitionModelTestStrategy(AdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ModelType getSupportedType() {
        return ModelType.IMAGE_RECOGNITION;
    }

    @Override
    public TestModelResponse test(AiModel model, Provider provider, TestModelRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 验证图片输入
            if (request.getImage() == null || request.getImage().isBlank()) {
                return TestModelResponse.builder()
                        .success(false)
                        .errorMessage("图像识别需要提供图片")
                        .duration(0L)
                        .build();
            }

            // 构建AI请求 - 使用多模态消息
            String prompt = request.getText() != null ? request.getText() : "请描述这张图片的内容";
            AiRequest aiRequest = AiRequest.builder()
                    .model(model.getCode())
                    .messages(Collections.singletonList(Message.user(prompt)))
                    .image(request.getImage())
                    .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 1000)
                    .build();

            // 获取适配器并调用
            AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());
            AiResponse aiResponse = adapter.imageRecognition(model, aiRequest).block();

            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(true)
                    .duration(duration)
                    .content(aiResponse.getContent())
                    .inputTokens(aiResponse.getPromptTokens())
                    .outputTokens(aiResponse.getCompletionTokens())
                    .build();

        } catch (Exception e) {
            log.error("Image recognition model test failed: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .duration(duration)
                    .build();
        }
    }
}
