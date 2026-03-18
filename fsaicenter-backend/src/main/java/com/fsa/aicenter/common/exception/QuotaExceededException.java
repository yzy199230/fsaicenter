package com.fsa.aicenter.common.exception;

/**
 * 配额不足异常
 *
 * @author FSA AI Center
 */
public class QuotaExceededException extends BusinessException {

    public QuotaExceededException() {
        super(ErrorCode.API_KEY_QUOTA_EXCEEDED);
    }

    public QuotaExceededException(String message) {
        super(ErrorCode.API_KEY_QUOTA_EXCEEDED.getCode(), message);
    }

    public QuotaExceededException(long required, long available) {
        super(ErrorCode.API_KEY_QUOTA_EXCEEDED.getCode(),
              String.format("配额不足: 需要 %d, 剩余 %d", required, available));
    }
}
