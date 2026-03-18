package com.fsa.aicenter.infrastructure.adapter.test;

import com.fsa.aicenter.application.dto.request.TestModelRequest;
import com.fsa.aicenter.application.dto.response.TestModelResponse;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.model.entity.Provider;
import com.fsa.aicenter.domain.model.valueobject.ModelType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 模型测试策略接口
 *
 * @author FSA AI Center
 */
public interface ModelTestStrategy {

    /**
     * 获取支持的模型类型
     */
    ModelType getSupportedType();

    /**
     * 执行模型测试
     *
     * @param model    模型
     * @param provider 提供商
     * @param request  测试请求
     * @return 测试响应
     */
    TestModelResponse test(AiModel model, Provider provider, TestModelRequest request);

    /**
     * 执行流式模型测试
     *
     * @param model    模型
     * @param provider 提供商
     * @param request  测试请求
     * @return SSE发射器
     */
    default SseEmitter testStream(AiModel model, Provider provider, TestModelRequest request) {
        throw new UnsupportedOperationException("该模型类型不支持流式测试");
    }
}
