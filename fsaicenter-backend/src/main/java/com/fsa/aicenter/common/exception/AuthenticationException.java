package com.fsa.aicenter.common.exception;

/**
 * 认证异常
 *
 * @author FSA AI Center
 */
public class AuthenticationException extends BusinessException {

    public AuthenticationException(String message) {
        super(ErrorCode.UNAUTHORIZED.getCode(), message);
    }

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthenticationException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }
}
