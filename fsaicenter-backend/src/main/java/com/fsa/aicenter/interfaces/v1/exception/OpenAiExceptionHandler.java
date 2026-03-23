package com.fsa.aicenter.interfaces.v1.exception;

import com.fsa.aicenter.common.exception.AuthenticationException;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.QuotaExceededException;
import com.fsa.aicenter.common.exception.RateLimitException;
import com.fsa.aicenter.interfaces.v1.dto.OpenAiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * OpenAI 兼容错误格式处理器
 * 仅作用于 /v1/ 路径下的控制器
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.fsa.aicenter.interfaces.v1")
public class OpenAiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<OpenAiErrorResponse> handleAuthenticationException(AuthenticationException e) {
        log.warn("OpenAI API auth failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(OpenAiErrorResponse.of(e.getMessage(), "invalid_request_error", "invalid_api_key"));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<OpenAiErrorResponse> handleRateLimitException(RateLimitException e) {
        log.warn("OpenAI API rate limit: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(OpenAiErrorResponse.of(e.getMessage(), "rate_limit_error", "rate_limit_exceeded"));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<OpenAiErrorResponse> handleQuotaExceededException(QuotaExceededException e) {
        log.warn("OpenAI API quota exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(OpenAiErrorResponse.of(e.getMessage(), "insufficient_quota", "insufficient_quota"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OpenAiErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(OpenAiErrorResponse.of(message, "invalid_request_error", "invalid_request"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<OpenAiErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(OpenAiErrorResponse.of(e.getMessage(), "invalid_request_error", "invalid_request"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<OpenAiErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("OpenAI API business error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(OpenAiErrorResponse.of(e.getMessage(), "invalid_request_error", "invalid_request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<OpenAiErrorResponse> handleException(Exception e) {
        log.error("OpenAI API internal error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OpenAiErrorResponse.of("Internal server error", "server_error", "internal_error"));
    }
}
