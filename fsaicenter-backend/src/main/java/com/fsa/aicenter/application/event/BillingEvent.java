package com.fsa.aicenter.application.event;

import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 计费事件
 * <p>
 * 当AI请求完成后发布此事件，用于异步创建计费记录。
 * </p>
 *
 * @author FSA AI Center
 */
@Getter
public class BillingEvent {

    /**
     * 请求ID
     */
    private final String requestId;

    /**
     * API密钥
     */
    private final ApiKey apiKey;

    /**
     * 使用的模型
     */
    private final AiModel model;

    /**
     * Token数（Chat/Embeddings使用）
     */
    private final Integer tokens;

    /**
     * 成本金额（Image等使用）
     */
    private final Integer cost;

    /**
     * 请求时间
     */
    private final LocalDateTime requestTime;

    /**
     * 响应时间
     */
    private final LocalDateTime responseTime;

    /**
     * 事件创建时间
     */
    private final LocalDateTime eventTime;

    /**
     * 构造器（用于Token计费）
     */
    public BillingEvent(String requestId, ApiKey apiKey, AiModel model,
                       Integer tokens, LocalDateTime requestTime, LocalDateTime responseTime) {
        this(requestId, apiKey, model, tokens, null, requestTime, responseTime);
    }

    /**
     * 完整构造器
     */
    public BillingEvent(String requestId, ApiKey apiKey, AiModel model,
                       Integer tokens, Integer cost,
                       LocalDateTime requestTime, LocalDateTime responseTime) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        if (apiKey == null) {
            throw new IllegalArgumentException("API key cannot be null");
        }
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        if (tokens == null && cost == null) {
            throw new IllegalArgumentException("Either tokens or cost must be provided");
        }
        if (requestTime == null) {
            throw new IllegalArgumentException("Request time cannot be null");
        }
        if (responseTime == null) {
            throw new IllegalArgumentException("Response time cannot be null");
        }

        this.requestId = requestId;
        this.apiKey = apiKey;
        this.model = model;
        this.tokens = tokens;
        this.cost = cost;
        this.requestTime = requestTime;
        this.responseTime = responseTime;
        this.eventTime = LocalDateTime.now();
    }

    /**
     * 获取使用量（tokens或cost）
     */
    public long getUsageAmount() {
        if (tokens != null) {
            return tokens;
        }
        if (cost != null) {
            return cost;
        }
        return 0;
    }

    /**
     * 是否基于Token计费
     */
    public boolean isTokenBased() {
        return tokens != null;
    }

    @Override
    public String toString() {
        return "BillingEvent{" +
                "requestId='" + requestId + '\'' +
                ", apiKeyId=" + apiKey.getId() +
                ", modelId=" + model.getId() +
                ", tokens=" + tokens +
                ", cost=" + cost +
                ", eventTime=" + eventTime +
                '}';
    }
}
