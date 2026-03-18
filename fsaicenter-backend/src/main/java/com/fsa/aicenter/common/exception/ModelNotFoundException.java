package com.fsa.aicenter.common.exception;

/**
 * 模型不存在异常
 *
 * @author FSA AI Center
 */
public class ModelNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ModelNotFoundException(String modelCode) {
        super(ErrorCode.MODEL_NOT_FOUND, "模型不存在: " + modelCode);
    }

    public ModelNotFoundException(Long modelId) {
        super(ErrorCode.MODEL_NOT_FOUND, "模型不存在，ID: " + modelId);
    }
}
