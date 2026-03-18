package com.fsa.aicenter.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsa.aicenter.application.event.RequestLogEvent;
import com.fsa.aicenter.domain.log.aggregate.RequestLog;
import com.fsa.aicenter.domain.log.repository.RequestLogRepository;
import com.fsa.aicenter.domain.log.valueobject.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 请求日志事件监听器
 * <p>
 * 使用Virtual Thread异步处理日志事件，不阻塞主流程。
 * 主要职责：
 * 1. 从事件中提取日志信息
 * 2. 创建请求日志记录
 * 3. 保存日志详情（请求/响应内容）
 * 4. 支持分级存储（热数据/冷数据）
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class RequestLogEventListener {

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 处理请求日志事件
     * <p>
     * 使用@Async启用异步处理（Virtual Thread），@Transactional确保事务一致性。
     * 异常不会影响主流程，但会被记录。
     * </p>
     *
     * @param event 请求日志事件
     */
    @Async
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleRequestLogEvent(RequestLogEvent event) {
        if (event == null) {
            log.warn("收到空日志事件，跳过处理");
            return;
        }

        log.debug("开始处理日志事件: requestId={}, apiKeyId={}, endpoint={}, success={}",
                event.getRequestId(), event.getApiKey().getId(), event.getEndpoint(), event.getSuccess());

        try {
            // 1. 创建请求日志
            RequestLog requestLog = createRequestLog(event);

            // 2. 保存日志记录
            requestLogRepository.save(requestLog);

            // 3. 保存日志详情（请求/响应内容）
            saveLogDetail(event);

            // 4. 计算处理耗时
            long processingTime = Duration.between(event.getEventTime(), LocalDateTime.now()).toMillis();

            log.info("日志事件处理成功: requestId={}, logId={}, processingTime={}ms",
                    event.getRequestId(), requestLog.getId(), processingTime);

        } catch (Exception e) {
            log.error("日志事件处理失败: requestId={}", event.getRequestId(), e);
            // 日志处理失败不应该影响主流程
            // 但仍然抛出异常以触发事务回滚，确保数据一致性
            throw e;
        }
    }

    /**
     * 从事件创建RequestLog聚合根
     */
    private RequestLog createRequestLog(RequestLogEvent event) {
        // 构建TokenUsage
        TokenUsage tokenUsage = TokenUsage.zero();
        if (event.getTokens() != null && event.getTokens() > 0) {
            // 简化实现：总token数，实际应该区分prompt和completion
            tokenUsage = new TokenUsage(0, 0, event.getTokens());
        }

        if (event.getSuccess()) {
            // 成功日志
            return RequestLog.createSuccess(
                    event.getRequestId(),
                    event.getApiKey().getId(),
                    event.getModel() != null ? event.getModel().getId() : null,
                    event.getRequestType(),
                    event.getIsStream(),
                    tokenUsage,
                    event.getDuration() != null ? event.getDuration().intValue() : 0,
                    event.getClientIp(),
                    event.getUserAgent(),
                    event.getHttpStatus()
            );
        } else {
            // 失败日志
            return RequestLog.createFailure(
                    event.getRequestId(),
                    event.getApiKey().getId(),
                    event.getModel() != null ? event.getModel().getId() : null,
                    event.getRequestType(),
                    event.getHttpStatus(),
                    event.getErrorMessage(),
                    event.getDuration() != null ? event.getDuration().intValue() : 0,
                    event.getClientIp(),
                    event.getUserAgent()
            );
        }
    }

    /**
     * 保存日志详情（请求/响应内容）
     * <p>
     * 将请求和响应对象序列化为JSON字符串，存储到JSONB字段。
     * 支持分级存储：
     * - 热数据（近7天）：保存完整详情
     * - 冷数据（>7天）：可选择性保存或压缩
     * </p>
     */
    private void saveLogDetail(RequestLogEvent event) {
        try {
            String requestBody = serializeToJson(event.getRequest());
            String responseBody = serializeToJson(event.getResponse());
            String requestHeaders = null; // 可以从HttpServletRequest中提取

            requestLogRepository.saveDetail(
                    event.getRequestId(),
                    requestBody,
                    responseBody,
                    requestHeaders
            );

            log.debug("日志详情已保存: requestId={}, requestSize={}, responseSize={}",
                    event.getRequestId(),
                    requestBody != null ? requestBody.length() : 0,
                    responseBody != null ? responseBody.length() : 0);

        } catch (Exception e) {
            // 详情保存失败不应影响主记录
            log.warn("保存日志详情失败: requestId={}", event.getRequestId(), e);
        }
    }

    /**
     * 将对象序列化为JSON字符串
     * <p>
     * 处理序列化异常，返回错误信息而不是抛出异常。
     * 对于流式响应，可能需要特殊处理（只记录摘要）。
     * </p>
     */
    private String serializeToJson(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            // 对于大对象，可以考虑限制大小或只保存摘要
            String json = objectMapper.writeValueAsString(obj);

            // 限制单个字段最大1MB
            final int maxSize = 1024 * 1024;
            if (json.length() > maxSize) {
                log.warn("JSON内容过大，截断: originalSize={}, maxSize={}", json.length(), maxSize);
                json = json.substring(0, maxSize) + "... [truncated]";
            }

            return json;
        } catch (JsonProcessingException e) {
            log.warn("对象序列化为JSON失败: {}", obj.getClass().getSimpleName(), e);
            return "{\"error\": \"Failed to serialize: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 判断是否为慢请求
     * <p>
     * 可以对慢请求进行特殊处理：告警、详细分析等。
     * </p>
     */
    @SuppressWarnings("unused")
    private boolean isSlowRequest(RequestLogEvent event) {
        if (event.getDuration() == null) {
            return false;
        }

        // 根据请求类型定义不同的慢请求阈值
        long thresholdMs;
        switch (event.getRequestType()) {
            case CHAT:
            case IMAGE:
                thresholdMs = 5000; // 5秒
                break;
            case EMBEDDING:
                thresholdMs = 2000; // 2秒
                break;
            default:
                thresholdMs = 3000; // 默认3秒
        }

        return event.getDuration() > thresholdMs;
    }

    /**
     * 判断是否需要详细日志
     * <p>
     * 根据业务需求，某些情况下可以跳过详细日志：
     * - 成功的健康检查请求
     * - 简单的查询请求
     * - 配置的忽略路径
     * </p>
     */
    @SuppressWarnings("unused")
    private boolean shouldSaveDetail(RequestLogEvent event) {
        // 失败请求总是保存详情
        if (!event.getSuccess()) {
            return true;
        }

        // 慢请求总是保存详情
        if (isSlowRequest(event)) {
            return true;
        }

        // 其他请求根据配置决定
        // 可以从配置文件读取：log.detail.enabled=true
        return true;
    }
}
