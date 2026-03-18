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
 * 图像生成模型测试策略
 *
 * @author FSA AI Center
 */
@Component
public class ImageModelTestStrategy implements ModelTestStrategy {

    private static final Logger log = LoggerFactory.getLogger(ImageModelTestStrategy.class);

    private final AdapterFactory adapterFactory;

    public ImageModelTestStrategy(AdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ModelType getSupportedType() {
        return ModelType.IMAGE;
    }

    @Override
    public TestModelResponse test(AiModel model, Provider provider, TestModelRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 判断是文生图还是图生图
            boolean isImageToImage = request.getImage() != null;

            // 获取prompt：优先使用prompt字段，如果为空则使用text字段
            String prompt = request.getPrompt() != null ? request.getPrompt() : request.getText();

            // 构建请求
            AiRequest.AiRequestBuilder builder = AiRequest.builder()
                    .model(model.getCode())
                    .prompt(prompt);

            if (isImageToImage) {
                builder.image(request.getImage());
            }

            AiRequest aiRequest = builder.build();

            // 获取适配器并调用
            AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());
            AiResponse aiResponse = adapter.generateImage(model, aiRequest).block();

            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(true)
                    .duration(duration)
                    .imageUrls(aiResponse.getImageUrls())
                    .build();

        } catch (Exception e) {
            log.error("Image model test failed: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .duration(duration)
                    .build();
        }
    }
}
