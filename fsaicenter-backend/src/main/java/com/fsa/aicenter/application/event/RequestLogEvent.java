package com.fsa.aicenter.application.event;

import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.log.valueobject.RequestType;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 请求日志事件
 * <p>
 * 当AI请求完成或失败后发布此事件，用于异步记录请求日志。
 * </p>
 *
 * @author FSA AI Center
 */
@Getter
public class RequestLogEvent {

    /**
     * 请求ID
     */
    private final String requestId;

    /**
     * API密钥
     */
    private final ApiKey apiKey;

    /**
     * 使用的模型（失败时可能为null）
     */
    private final AiModel model;

    /**
     * 端点路径
     */
    private final String endpoint;

    /**
     * 请求类型
     */
    private final RequestType requestType;

    /**
     * 是否流式请求
     */
    private final Boolean isStream;

    /**
     * 请求参数（序列化为JSON）
     */
    private final Object request;

    /**
     * 响应结果（序列化为JSON）
     */
    private final Object response;

    /**
     * 请求耗时（毫秒）
     */
    private final Long duration;

    /**
     * 是否成功
     */
    private final Boolean success;

    /**
     * HTTP状态码
     */
    private final Integer httpStatus;

    /**
     * 错误信息
     */
    private final String errorMessage;

    /**
     * 客户端IP
     */
    private final String clientIp;

    /**
     * User-Agent
     */
    private final String userAgent;

    /**
     * Token使用量
     */
    private final Integer tokens;

    /**
     * 输入Token数
     */
    private final Integer promptTokens;

    /**
     * 输出Token数
     */
    private final Integer completionTokens;

    /**
     * 事件创建时间
     */
    private final LocalDateTime eventTime;

    /**
     * 构造器（成功请求）
     */
    private RequestLogEvent(Builder builder) {
        if (builder.requestId == null || builder.requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        if (builder.apiKey == null) {
            throw new IllegalArgumentException("API key cannot be null");
        }
        if (builder.endpoint == null || builder.endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        if (builder.requestType == null) {
            throw new IllegalArgumentException("Request type cannot be null");
        }

        this.requestId = builder.requestId;
        this.apiKey = builder.apiKey;
        this.model = builder.model;
        this.endpoint = builder.endpoint;
        this.requestType = builder.requestType;
        this.isStream = builder.isStream != null ? builder.isStream : false;
        this.request = builder.request;
        this.response = builder.response;
        this.duration = builder.duration;
        this.success = builder.success != null ? builder.success : true;
        this.httpStatus = builder.httpStatus != null ? builder.httpStatus : 200;
        this.errorMessage = builder.errorMessage;
        this.clientIp = builder.clientIp;
        this.userAgent = builder.userAgent;
        this.tokens = builder.tokens;
        this.promptTokens = builder.promptTokens;
        this.completionTokens = builder.completionTokens;
        this.eventTime = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder模式
     */
    public static class Builder {
        private String requestId;
        private ApiKey apiKey;
        private AiModel model;
        private String endpoint;
        private RequestType requestType;
        private Boolean isStream;
        private Object request;
        private Object response;
        private Long duration;
        private Boolean success;
        private Integer httpStatus;
        private String errorMessage;
        private String clientIp;
        private String userAgent;
        private Integer tokens;
        private Integer promptTokens;
        private Integer completionTokens;

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder apiKey(ApiKey apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder model(AiModel model) {
            this.model = model;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder requestType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        public Builder isStream(Boolean isStream) {
            this.isStream = isStream;
            return this;
        }

        public Builder request(Object request) {
            this.request = request;
            return this;
        }

        public Builder response(Object response) {
            this.response = response;
            return this;
        }

        public Builder duration(Long duration) {
            this.duration = duration;
            return this;
        }

        public Builder success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder httpStatus(Integer httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder tokens(Integer tokens) {
            this.tokens = tokens;
            return this;
        }

        public Builder promptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public Builder completionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        public RequestLogEvent build() {
            return new RequestLogEvent(this);
        }
    }

    @Override
    public String toString() {
        return "RequestLogEvent{" +
                "requestId='" + requestId + '\'' +
                ", apiKeyId=" + apiKey.getId() +
                ", modelId=" + (model != null ? model.getId() : null) +
                ", endpoint='" + endpoint + '\'' +
                ", requestType=" + requestType +
                ", success=" + success +
                ", duration=" + duration +
                ", eventTime=" + eventTime +
                '}';
    }
}
