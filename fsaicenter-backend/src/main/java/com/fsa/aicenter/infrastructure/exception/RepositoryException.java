package com.fsa.aicenter.infrastructure.exception;

/**
 * Repository层异常
 * 用于封装数据持久化过程中的异常
 */
public class RepositoryException extends RuntimeException {

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
