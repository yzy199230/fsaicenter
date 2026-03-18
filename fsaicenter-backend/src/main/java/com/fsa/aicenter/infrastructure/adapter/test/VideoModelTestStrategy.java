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
 * 视频生成模型测试策略
 *
 * @author FSA AI Center
 */
@Component
public class VideoModelTestStrategy implements ModelTestStrategy {

    private static final Logger log = LoggerFactory.getLogger(VideoModelTestStrategy.class);

    private final AdapterFactory adapterFactory;

    public VideoModelTestStrategy(AdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @Override
    public ModelType getSupportedType() {
        return ModelType.VIDEO;
    }

    @Override
    public TestModelResponse test(AiModel model, Provider provider, TestModelRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 验证输入 - 文生视频需要文本，图生视频需要图片
            boolean hasText = request.getText() != null && !request.getText().isBlank();
            boolean hasImage = request.getImage() != null && !request.getImage().isBlank();

            if (!hasText && !hasImage) {
                return TestModelResponse.builder()
                        .success(false)
                        .errorMessage("视频生成需要提供文本描述或参考图片")
                        .duration(0L)
                        .build();
            }

            // 构建AI请求
            AiRequest.AiRequestBuilder builder = AiRequest.builder()
                    .model(model.getCode());

            if (hasText) {
                builder.prompt(request.getText());
            }
            if (hasImage) {
                builder.image(request.getImage());
            }

            AiRequest aiRequest = builder.build();

            // 获取适配器并调用
            AiProviderAdapter adapter = adapterFactory.getAdapter(provider.getCode());
            AiResponse aiResponse = adapter.generateVideo(model, aiRequest).block();

            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(true)
                    .duration(duration)
                    .videoUrl(aiResponse.getVideoUrl())
                    .build();

        } catch (Exception e) {
            log.error("Video model test failed: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;

            return TestModelResponse.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .duration(duration)
                    .build();
        }
    }
}
