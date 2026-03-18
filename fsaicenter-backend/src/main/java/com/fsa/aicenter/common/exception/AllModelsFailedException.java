package com.fsa.aicenter.common.exception;

import com.fsa.aicenter.domain.model.valueobject.ModelType;

/**
 * 所有模型调用失败异常
 * <p>
 * 当降级策略中的所有模型都调用失败时抛出此异常
 * </p>
 *
 * @author FSA AI Center
 */
public class AllModelsFailedException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public AllModelsFailedException(ModelType modelType, int attemptCount) {
        super(ErrorCode.MODEL_FALLBACK_FAILED,
                String.format("所有%s模型均调用失败，已尝试%d个模型", modelType.getDesc(), attemptCount));
    }

    public AllModelsFailedException(ModelType modelType, int attemptCount, Throwable lastError) {
        super(ErrorCode.MODEL_FALLBACK_FAILED,
                String.format("所有%s模型均调用失败，已尝试%d个模型，最后错误: %s",
                        modelType.getDesc(), attemptCount, lastError.getMessage()));
    }

    public AllModelsFailedException(String message) {
        super(ErrorCode.MODEL_FALLBACK_FAILED, message);
    }
}
