package com.fsa.aicenter.common.exception;

import com.fsa.aicenter.domain.model.valueobject.ModelType;

/**
 * 无可用模型异常
 * <p>
 * 当指定类型的所有模型都不可用时抛出此异常
 * </p>
 *
 * @author FSA AI Center
 */
public class NoAvailableModelException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public NoAvailableModelException(ModelType modelType) {
        super(ErrorCode.MODEL_NOT_FOUND,
                String.format("该类型没有可用的模型: %s (%s)", modelType.getDesc(), modelType.getCode()));
    }

    public NoAvailableModelException(String message) {
        super(ErrorCode.MODEL_NOT_FOUND, message);
    }
}
