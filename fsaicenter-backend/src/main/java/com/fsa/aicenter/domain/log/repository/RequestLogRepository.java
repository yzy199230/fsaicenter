package com.fsa.aicenter.domain.log.repository;

import com.fsa.aicenter.domain.log.aggregate.RequestLog;
import com.fsa.aicenter.domain.log.valueobject.LogStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 请求日志仓储接口
 */
public interface RequestLogRepository {
    Optional<RequestLog> findById(Long id);
    Optional<RequestLog> findByRequestId(String requestId);

    /**
     * 按API密钥ID查询日志
     * 警告：可能返回大量数据，调用方应限制时间范围（建议不超过7天）
     * TODO: 未来版本改为分页查询
     */
    List<RequestLog> findByApiKeyId(Long apiKeyId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按模型ID查询日志
     * 警告：可能返回大量数据，调用方应限制时间范围（建议不超过7天）
     * TODO: 未来版本改为分页查询
     */
    List<RequestLog> findByModelId(Long modelId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按状态查询日志
     * 警告：可能返回大量数据，调用方应限制时间范围（建议不超过7天）
     * TODO: 未来版本改为分页查询
     */
    List<RequestLog> findByStatus(LogStatus status, LocalDateTime startTime, LocalDateTime endTime);

    void save(RequestLog log);
    void batchSave(List<RequestLog> logs);

    /**
     * 保存请求日志详情
     *
     * @param requestId 请求ID
     * @param requestBody 请求体JSON字符串
     * @param responseBody 响应体JSON字符串
     * @param requestHeaders 请求头JSON字符串
     */
    void saveDetail(String requestId, String requestBody, String responseBody, String requestHeaders);

    /**
     * 根据请求ID查询日志详情
     *
     * @param requestId 请求ID
     * @return Map包含requestBody, responseBody, requestHeaders（键值均为JSON字符串）
     */
    Optional<Map<String, String>> findDetailByRequestId(String requestId);
}
