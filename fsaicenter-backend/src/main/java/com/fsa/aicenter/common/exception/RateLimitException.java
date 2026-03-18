package com.fsa.aicenter.common.exception;

/**
 * 限流异常
 *
 * @author FSA AI Center
 */
public class RateLimitException extends BusinessException {

    public RateLimitException(String message) {
        super(ErrorCode.TOO_MANY_REQUESTS.getCode(), message);
    }

    public RateLimitException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RateLimitException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
