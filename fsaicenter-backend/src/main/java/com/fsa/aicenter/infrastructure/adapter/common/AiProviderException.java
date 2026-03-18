package com.fsa.aicenter.infrastructure.adapter.common;

import lombok.Getter;

/**
 * AI提供商适配器异常
 * <p>
 * 在调用AI提供商API过程中发生的各类异常的统一封装
 * </p>
 */
@Getter
public class AiProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 提供商代码
     */
    private final String providerCode;

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * HTTP状态码（如果适用）
     */
    private final Integer httpStatus;

    /**
     * 是否可重试
     */
    private final boolean retryable;

    /**
     * 构造函数
     *
     * @param providerCode 提供商代码
     * @param message      错误消息
     */
    public AiProviderException(String providerCode, String message) {
        this(providerCode, message, null, null, null, false);
    }

    /**
     * 构造函数
     *
     * @param providerCode 提供商代码
     * @param message      错误消息
     * @param cause        原始异常
     */
    public AiProviderException(String providerCode, String message, Throwable cause) {
        this(providerCode, message, cause, null, null, false);
    }

    /**
     * 完整构造函数
     *
     * @param providerCode 提供商代码
     * @param message      错误消息
     * @param cause        原始异常
     * @param errorCode    错误代码
     * @param httpStatus   HTTP状态码
     * @param retryable    是否可重试
     */
    public AiProviderException(String providerCode, String message, Throwable cause,
                               String errorCode, Integer httpStatus, boolean retryable) {
        super(buildMessage(providerCode, message, errorCode, httpStatus), cause);
        this.providerCode = providerCode;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    /**
     * 构建异常消息
     */
    private static String buildMessage(String providerCode, String message,
                                        String errorCode, Integer httpStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(providerCode).append("]");

        if (errorCode != null) {
            sb.append(" ").append(errorCode);
        }

        if (httpStatus != null) {
            sb.append(" (HTTP ").append(httpStatus).append(")");
        }

        sb.append(": ").append(message);

        return sb.toString();
    }

    // ========== 静态工厂方法 ==========

    /**
     * 创建网络异常
     */
    public static AiProviderException networkError(String providerCode, String message, Throwable cause) {
        return new AiProviderException(providerCode, message, cause, "NETWORK_ERROR", null, true);
    }

    /**
     * 创建超时异常
     */
    public static AiProviderException timeout(String providerCode, String message) {
        return new AiProviderException(providerCode, message, null, "TIMEOUT", null, true);
    }

    /**
     * 创建认证异常
     */
    public static AiProviderException authError(String providerCode, String message) {
        return new AiProviderException(providerCode, message, null, "AUTH_ERROR", 401, false);
    }

    /**
     * 创建限流异常
     */
    public static AiProviderException rateLimitError(String providerCode, String message) {
        return new AiProviderException(providerCode, message, null, "RATE_LIMIT", 429, true);
    }

    /**
     * 创建服务器错误异常
     */
    public static AiProviderException serverError(String providerCode, String message, Integer httpStatus) {
        boolean retryable = httpStatus != null && httpStatus >= 500;
        return new AiProviderException(providerCode, message, null, "SERVER_ERROR", httpStatus, retryable);
    }

    /**
     * 创建参数错误异常
     */
    public static AiProviderException invalidRequest(String providerCode, String message) {
        return new AiProviderException(providerCode, message, null, "INVALID_REQUEST", 400, false);
    }

    /**
     * 创建模型不存在异常
     */
    public static AiProviderException modelNotFound(String providerCode, String modelName) {
        return new AiProviderException(providerCode,
                "Model not found: " + modelName, null, "MODEL_NOT_FOUND", 404, false);
    }

    /**
     * 是否为限流错误
     */
    public boolean isRateLimitError() {
        return "RATE_LIMIT".equals(errorCode);
    }

    /**
     * 是否为认证错误
     */
    public boolean isAuthError() {
        return "AUTH_ERROR".equals(errorCode);
    }

    /**
     * 是否为服务器错误
     */
    public boolean isServerError() {
        return httpStatus != null && httpStatus >= 500;
    }
}
