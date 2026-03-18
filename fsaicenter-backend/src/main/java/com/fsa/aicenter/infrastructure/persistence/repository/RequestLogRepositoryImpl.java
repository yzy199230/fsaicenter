package com.fsa.aicenter.infrastructure.persistence.repository;

import com.fsa.aicenter.domain.log.aggregate.RequestLog;
import com.fsa.aicenter.domain.log.repository.RequestLogRepository;
import com.fsa.aicenter.domain.log.valueobject.LogStatus;
import com.fsa.aicenter.domain.log.valueobject.RequestType;
import com.fsa.aicenter.domain.log.valueobject.TokenUsage;
import com.fsa.aicenter.infrastructure.exception.RepositoryException;
import com.fsa.aicenter.infrastructure.persistence.entity.RequestLogDetailPO;
import com.fsa.aicenter.infrastructure.persistence.entity.RequestLogPO;
import com.fsa.aicenter.infrastructure.persistence.mapper.RequestLogDetailMapper;
import com.fsa.aicenter.infrastructure.persistence.mapper.RequestLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 请求日志仓储实现类
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RequestLogRepositoryImpl implements RequestLogRepository {

    private static final int BATCH_SIZE = 1000;  // 批量操作大小

    private final RequestLogMapper requestLogMapper;
    private final RequestLogDetailMapper requestLogDetailMapper;

    @Override
    public Optional<RequestLog> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Request log ID cannot be null");
        }

        RequestLogPO po = requestLogMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<RequestLog> findByRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        RequestLogPO po = requestLogMapper.selectByRequestId(requestId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<RequestLog> findByApiKeyId(Long apiKeyId, LocalDateTime startTime, LocalDateTime endTime) {
        if (apiKeyId == null) {
            throw new IllegalArgumentException("API key ID cannot be null");
        }
        validateTimeRange(startTime, endTime);

        List<RequestLogPO> poList = requestLogMapper.selectByApiKeyIdAndTimeRange(
            apiKeyId, startTime, endTime
        );

        return poList.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<RequestLog> findByModelId(Long modelId, LocalDateTime startTime, LocalDateTime endTime) {
        if (modelId == null) {
            throw new IllegalArgumentException("Model ID cannot be null");
        }
        validateTimeRange(startTime, endTime);

        List<RequestLogPO> poList = requestLogMapper.selectByModelIdAndTimeRange(
            modelId, startTime, endTime
        );

        return poList.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<RequestLog> findByStatus(LogStatus status, LocalDateTime startTime, LocalDateTime endTime) {
        if (status == null) {
            throw new IllegalArgumentException("Log status cannot be null");
        }
        validateTimeRange(startTime, endTime);

        List<RequestLogPO> poList = requestLogMapper.selectByStatusAndTimeRange(
            status.getCode(), startTime, endTime
        );

        return poList.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void save(RequestLog requestLog) {
        if (requestLog == null) {
            throw new IllegalArgumentException("Request log cannot be null");
        }

        log.info("Saving request log: requestId={}, apiKeyId={}, modelId={}, status={}",
            requestLog.getRequestId(), requestLog.getApiKeyId(), requestLog.getModelId(), requestLog.getStatus());

        RequestLogPO po = toPO(requestLog);
        requestLogMapper.insert(po);
        requestLog.setId(po.getId());

        log.debug("Request log saved with id={}", po.getId());
    }

    /**
     * 批量保存请求日志
     *
     * @param logs 请求日志列表
     * @throws IllegalArgumentException 如果logs为null
     * @apiNote 如果logs为空列表，方法将静默返回，不执行任何操作
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchSave(List<RequestLog> logs) {
        if (logs == null) {
            throw new IllegalArgumentException("Logs cannot be null");
        }
        if (logs.isEmpty()) {
            log.debug("Batch save called with empty list, skipping");
            return;
        }

        log.info("Batch saving {} request logs", logs.size());

        // 转换为PO列表
        List<RequestLogPO> poList = logs.stream()
            .map(this::toPO)
            .collect(Collectors.toList());

        // 分批插入（每批BATCH_SIZE条）
        int totalBatches = (poList.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        for (int i = 0; i < poList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, poList.size());
            List<RequestLogPO> batch = poList.subList(i, end);

            // 批量插入
            requestLogMapper.insertBatch(batch);

            log.debug("Batch inserted {} logs (batch {}/{})",
                batch.size(), (i / BATCH_SIZE) + 1, totalBatches);
        }

        // 回设ID到原对象（检查ID是否成功生成）
        for (int i = 0; i < logs.size(); i++) {
            Long generatedId = poList.get(i).getId();
            if (generatedId != null) {
                logs.get(i).setId(generatedId);
            } else {
                log.warn("Failed to get generated ID for batch insert at index {}", i);
            }
        }

        log.info("Batch saved {} request logs", logs.size());
    }

    // ========== 辅助方法 ==========

    /**
     * 校验时间范围参数
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    // ========== 转换方法 ==========

    /**
     * RequestLog: PO → 领域对象
     */
    private RequestLog toDomain(RequestLogPO po) {
        try {
            RequestLog log = new RequestLog();
            log.setId(po.getId());
            log.setRequestId(po.getRequestId());
            log.setApiKeyId(po.getApiKeyId());
            log.setModelId(po.getModelId());
            log.setRequestType(RequestType.fromCode(po.getRequestType()));
            log.setIsStream(po.getIsStream());
            log.setTokenUsage(new TokenUsage(po.getPromptTokens(), po.getCompletionTokens(), po.getTotalTokens()));
            log.setRequestIp(po.getRequestIp());
            log.setUserAgent(po.getUserAgent());
            log.setHttpStatus(po.getHttpStatus());
            log.setResponseTimeMs(po.getResponseTimeMs());
            log.setErrorMessage(po.getErrorMessage());
            log.setStatus(LogStatus.fromCode(po.getStatus()));
            log.setCreatedTime(po.getCreatedTime());

            return log;
        } catch (Exception e) {
            log.error("Failed to convert RequestLogPO to domain: id={}", po.getId(), e);
            throw new RepositoryException("Failed to convert request log: " + po.getId(), e);
        }
    }

    /**
     * RequestLog: 领域对象 → PO
     */
    private RequestLogPO toPO(RequestLog log) {
        RequestLogPO po = new RequestLogPO();
        po.setId(log.getId());
        po.setRequestId(log.getRequestId());
        po.setApiKeyId(log.getApiKeyId());
        po.setModelId(log.getModelId());
        po.setRequestType(log.getRequestType().getCode());
        po.setIsStream(log.getIsStream());
        // TokenUsage值对象展开，添加null保护
        TokenUsage usage = log.getTokenUsage();
        if (usage != null) {
            po.setPromptTokens(usage.getPromptTokens());
            po.setCompletionTokens(usage.getCompletionTokens());
            po.setTotalTokens(usage.getTotalTokens());
        } else {
            // 如果没有token使用情况，设置为0
            po.setPromptTokens(0);
            po.setCompletionTokens(0);
            po.setTotalTokens(0);
        }
        po.setRequestIp(log.getRequestIp());
        po.setUserAgent(log.getUserAgent());
        po.setHttpStatus(log.getHttpStatus());
        po.setResponseTimeMs(log.getResponseTimeMs());
        po.setErrorMessage(log.getErrorMessage());
        po.setStatus(log.getStatus().getCode());
        // 保留已有的createdTime，只有新建时才依赖数据库DEFAULT
        if (log.getCreatedTime() != null) {
            po.setCreatedTime(log.getCreatedTime());
        }
        // 否则由数据库DEFAULT CURRENT_TIMESTAMP处理

        return po;
    }

    // ========== 请求日志详情操作 ==========

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveDetail(String requestId, String requestBody, String responseBody, String requestHeaders) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        log.info("Saving request log detail: requestId={}", requestId);

        RequestLogDetailPO po = new RequestLogDetailPO();
        po.setRequestId(requestId);
        po.setRequestBody(requestBody);
        po.setResponseBody(responseBody);
        po.setRequestHeaders(requestHeaders);

        try {
            // 使用ON CONFLICT直接插入或更新
            requestLogDetailMapper.insertOrUpdate(po);
            log.info("Request log detail saved: requestId={}", requestId);
        } catch (Exception e) {
            log.error("Failed to save request log detail: requestId={}", requestId, e);
            throw new RepositoryException("Failed to save request log detail", e);
        }
    }

    @Override
    public Optional<Map<String, String>> findDetailByRequestId(String requestId) {
        // 参数验证
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        log.debug("Finding request log detail: requestId={}", requestId);

        try {
            RequestLogDetailPO po = requestLogDetailMapper.selectByRequestId(requestId);

            if (po == null) {
                log.debug("Request log detail not found: requestId={}", requestId);
                return Optional.empty();
            }

            // 转换为Map
            Map<String, String> detail = new HashMap<>();
            detail.put("requestBody", po.getRequestBody());
            detail.put("responseBody", po.getResponseBody());
            detail.put("requestHeaders", po.getRequestHeaders());

            log.debug("Request log detail found: requestId={}", requestId);
            return Optional.of(detail);

        } catch (Exception e) {
            log.error("Failed to find request log detail: requestId={}", requestId, e);
            throw new RepositoryException("Failed to find request log detail", e);
        }
    }
}
