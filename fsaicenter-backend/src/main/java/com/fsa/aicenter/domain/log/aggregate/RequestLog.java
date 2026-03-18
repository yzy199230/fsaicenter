package com.fsa.aicenter.domain.log.aggregate;

import com.fsa.aicenter.domain.log.valueobject.LogStatus;
import com.fsa.aicenter.domain.log.valueobject.RequestType;
import com.fsa.aicenter.domain.log.valueobject.TokenUsage;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 请求日志聚合根
 */
@Getter
@Setter
public class RequestLog {
    private Long id;
    private String requestId;
    private Long apiKeyId;
    private Long modelId;
    private RequestType requestType;
    private Boolean isStream;
    private TokenUsage tokenUsage;
    private String requestIp;
    private String userAgent;
    private Integer httpStatus;
    private Integer responseTimeMs;
    private String errorMessage;
    private LogStatus status;
    private LocalDateTime createdTime;

    /**
     * 无参构造器，仅供持久化框架使用
     */
    public RequestLog() {
    }

    // ========== 领域行为 ==========

    /**
     * 创建成功日志（静态工厂方法）
     */
    public static RequestLog createSuccess(String requestId, Long apiKeyId, Long modelId,
                                          RequestType requestType, Boolean isStream,
                                          TokenUsage tokenUsage, Integer responseTimeMs,
                                          String requestIp, String userAgent) {
        return createSuccess(requestId, apiKeyId, modelId, requestType, isStream,
                           tokenUsage, responseTimeMs, requestIp, userAgent, 200);
    }

    /**
     * 创建成功日志（完整参数版本）
     */
    public static RequestLog createSuccess(String requestId, Long apiKeyId, Long modelId,
                                          RequestType requestType, Boolean isStream,
                                          TokenUsage tokenUsage, Integer responseTimeMs,
                                          String requestIp, String userAgent, Integer httpStatus) {
        // 参数校验
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        if (apiKeyId == null) {
            throw new IllegalArgumentException("API key ID cannot be null");
        }
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }
        if (requestType == null) {
            throw new IllegalArgumentException("Request type cannot be null");
        }

        RequestLog log = new RequestLog();
        log.requestId = requestId;
        log.apiKeyId = apiKeyId;
        log.modelId = modelId;
        log.requestType = requestType;
        log.isStream = isStream != null ? isStream : false;
        log.tokenUsage = tokenUsage != null ? tokenUsage : TokenUsage.zero();
        log.responseTimeMs = responseTimeMs;
        log.requestIp = requestIp;
        log.userAgent = userAgent;
        log.httpStatus = httpStatus != null ? httpStatus : 200;
        log.status = LogStatus.SUCCESS;
        log.createdTime = LocalDateTime.now();
        return log;
    }

    /**
     * 创建失败日志（静态工厂方法）
     */
    public static RequestLog createFailure(String requestId, Long apiKeyId, Long modelId,
                                          RequestType requestType, Integer httpStatus,
                                          String errorMessage, Integer responseTimeMs,
                                          String requestIp, String userAgent) {
        // 参数校验
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        if (apiKeyId == null) {
            throw new IllegalArgumentException("API key ID cannot be null");
        }
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }
        if (requestType == null) {
            throw new IllegalArgumentException("Request type cannot be null");
        }

        RequestLog log = new RequestLog();
        log.requestId = requestId;
        log.apiKeyId = apiKeyId;
        log.modelId = modelId;
        log.requestType = requestType;
        log.isStream = false;
        log.tokenUsage = TokenUsage.zero();
        log.httpStatus = httpStatus;
        log.errorMessage = errorMessage;
        log.responseTimeMs = responseTimeMs;
        log.requestIp = requestIp;
        log.userAgent = userAgent;
        log.status = LogStatus.FAILURE;
        log.createdTime = LocalDateTime.now();
        return log;
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status != null && status.isSuccess();
    }

    /**
     * 是否流式请求
     */
    public boolean isStreamRequest() {
        return Boolean.TRUE.equals(isStream);
    }

    /**
     * 响应时间是否超过阈值（毫秒）
     */
    public boolean isSlowRequest(int thresholdMs) {
        return responseTimeMs != null && responseTimeMs > thresholdMs;
    }
}
