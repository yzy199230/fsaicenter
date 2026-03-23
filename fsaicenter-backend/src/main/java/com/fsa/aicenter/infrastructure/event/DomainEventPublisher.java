package com.fsa.aicenter.infrastructure.event;

import com.fsa.aicenter.application.event.BillingEvent;
import com.fsa.aicenter.application.event.RequestLogEvent;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.log.valueobject.RequestType;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 领域事件发布器
 * <p>
 * 统一的事件发布入口，封装Spring ApplicationEventPublisher。
 * 所有领域事件（计费、日志等）都通过此类发布，由对应的监听器异步处理。
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class DomainEventPublisher {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 发布计费事件
     * <p>
     * 用于Token计费（Chat、Embeddings等）
     * </p>
     *
     * @param requestId    请求ID
     * @param apiKey       API密钥
     * @param model        使用的模型
     * @param tokens       Token数量
     * @param requestTime  请求时间
     * @param responseTime 响应时间
     */
    public void publishBillingEvent(String requestId, ApiKey apiKey, AiModel model,
                                    Integer tokens, LocalDateTime requestTime, LocalDateTime responseTime) {
        if (requestId == null || apiKey == null || model == null) {
            log.warn("发布计费事件失败：必要参数为空");
            return;
        }

        try {
            BillingEvent event = new BillingEvent(requestId, apiKey, model, tokens, requestTime, responseTime);
            eventPublisher.publishEvent(event);
            log.debug("计费事件已发布: {}", event);
        } catch (Exception e) {
            log.error("发布计费事件异常: requestId={}", requestId, e);
        }
    }

    /**
     * 发布计费事件（完整版本）
     * <p>
     * 支持Token和成本两种计费方式（Image等使用成本计费）
     * </p>
     *
     * @param requestId    请求ID
     * @param apiKey       API密钥
     * @param model        使用的模型
     * @param tokens       Token数量（可选）
     * @param cost         成本金额（可选）
     * @param requestTime  请求时间
     * @param responseTime 响应时间
     */
    public void publishBillingEvent(String requestId, ApiKey apiKey, AiModel model,
                                    Integer tokens, Integer cost,
                                    LocalDateTime requestTime, LocalDateTime responseTime) {
        if (requestId == null || apiKey == null || model == null) {
            log.warn("发布计费事件失败：必要参数为空");
            return;
        }

        try {
            BillingEvent event = new BillingEvent(requestId, apiKey, model, tokens, cost, requestTime, responseTime);
            eventPublisher.publishEvent(event);
            log.debug("计费事件已发布: {}", event);
        } catch (Exception e) {
            log.error("发布计费事件异常: requestId={}", requestId, e);
        }
    }

    /**
     * 发布请求日志事件
     * <p>
     * 使用Builder模式构建，支持灵活的参数组合。
     * </p>
     *
     * @param eventBuilder 日志事件Builder
     */
    public void publishRequestLogEvent(RequestLogEvent.Builder eventBuilder) {
        if (eventBuilder == null) {
            log.warn("发布日志事件失败：eventBuilder为空");
            return;
        }

        try {
            RequestLogEvent event = eventBuilder.build();
            eventPublisher.publishEvent(event);
            log.debug("日志事件已发布: {}", event);
        } catch (Exception e) {
            log.error("发布日志事件异常", e);
        }
    }

    /**
     * 发布请求日志事件（简化版本 - 成功场景）
     *
     * @param requestId   请求ID
     * @param apiKey      API密钥
     * @param model       使用的模型
     * @param endpoint    端点路径
     * @param requestType 请求类型
     * @param isStream    是否流式
     * @param request     请求对象
     * @param response    响应对象
     * @param duration    耗时（毫秒）
     * @param tokens      Token使用量
     * @param clientIp    客户端IP
     * @param userAgent   User-Agent
     */
    public void publishSuccessLogEvent(String requestId, ApiKey apiKey, AiModel model,
                                       String endpoint, RequestType requestType, Boolean isStream,
                                       Object request, Object response, Long duration, Integer tokens,
                                       Integer promptTokens, Integer completionTokens,
                                       String clientIp, String userAgent) {
        publishRequestLogEvent(RequestLogEvent.builder()
                .requestId(requestId)
                .apiKey(apiKey)
                .model(model)
                .endpoint(endpoint)
                .requestType(requestType)
                .isStream(isStream)
                .request(request)
                .response(response)
                .duration(duration)
                .success(true)
                .httpStatus(200)
                .tokens(tokens)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .clientIp(clientIp)
                .userAgent(userAgent));
    }

    /**
     * 发布请求日志事件（简化版本 - 失败场景）
     *
     * @param requestId    请求ID
     * @param apiKey       API密钥
     * @param model        使用的模型（失败时可能为null）
     * @param endpoint     端点路径
     * @param requestType  请求类型
     * @param request      请求对象
     * @param httpStatus   HTTP状态码
     * @param errorMessage 错误信息
     * @param duration     耗时（毫秒）
     * @param clientIp     客户端IP
     * @param userAgent    User-Agent
     */
    public void publishFailureLogEvent(String requestId, ApiKey apiKey, AiModel model,
                                       String endpoint, RequestType requestType, Object request,
                                       Integer httpStatus, String errorMessage, Long duration,
                                       String clientIp, String userAgent) {
        publishRequestLogEvent(RequestLogEvent.builder()
                .requestId(requestId)
                .apiKey(apiKey)
                .model(model)
                .endpoint(endpoint)
                .requestType(requestType)
                .isStream(false)
                .request(request)
                .duration(duration)
                .success(false)
                .httpStatus(httpStatus)
                .errorMessage(errorMessage)
                .clientIp(clientIp)
                .userAgent(userAgent));
    }
}
