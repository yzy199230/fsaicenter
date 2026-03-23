package com.fsa.aicenter.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fsa.aicenter.application.dto.request.LogListQuery;
import com.fsa.aicenter.application.dto.response.LogDetailResponse;
import com.fsa.aicenter.application.dto.response.LogListResponse;
import com.fsa.aicenter.common.exception.BusinessException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.common.model.PageResult;
import com.fsa.aicenter.domain.log.repository.RequestLogRepository;
import com.fsa.aicenter.infrastructure.persistence.entity.ApiKeyPO;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelPO;
import com.fsa.aicenter.infrastructure.persistence.entity.RequestLogPO;
import com.fsa.aicenter.infrastructure.persistence.mapper.ApiKeyMapper;
import com.fsa.aicenter.infrastructure.persistence.mapper.ModelMapper;
import com.fsa.aicenter.infrastructure.persistence.mapper.RequestLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 日志查询服务
 *
 * @author FSA AI Center
 */
@Service
@RequiredArgsConstructor
public class LogQueryService {

    private final RequestLogMapper requestLogMapper;
    private final RequestLogRepository requestLogRepository;
    private final ApiKeyMapper apiKeyMapper;
    private final ModelMapper modelMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询日志列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    public PageResult<LogListResponse> listLogs(LogListQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<RequestLogPO> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(RequestLogPO::getRequestId, query.getKeyword()));
        }

        // 模型类型筛选（前端传PascalCase如Chat，数据库存UPPERCASE如CHAT）
        if (StringUtils.hasText(query.getModelType())) {
            wrapper.eq(RequestLogPO::getRequestType, query.getModelType().toUpperCase());
        }

        // 状态筛选
        if (StringUtils.hasText(query.getStatus())) {
            Integer statusValue = "SUCCESS".equalsIgnoreCase(query.getStatus()) ? 1 : 0;
            wrapper.eq(RequestLogPO::getStatus, statusValue);
        }

        // 时间范围筛选
        if (StringUtils.hasText(query.getStartTime())) {
            LocalDateTime startTime = LocalDateTime.parse(query.getStartTime(), FORMATTER);
            wrapper.ge(RequestLogPO::getCreatedTime, startTime);
        }
        if (StringUtils.hasText(query.getEndTime())) {
            LocalDateTime endTime = LocalDateTime.parse(query.getEndTime(), FORMATTER);
            wrapper.le(RequestLogPO::getCreatedTime, endTime);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(RequestLogPO::getCreatedTime);

        // 分页查询
        Page<RequestLogPO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<RequestLogPO> result = requestLogMapper.selectPage(page, wrapper);

        // 获取关联数据的名称映射
        Map<Long, String> apiKeyNameMap = getApiKeyNameMap();
        Map<Long, ModelInfo> modelInfoMap = getModelInfoMap();

        // 转换为响应对象
        List<LogListResponse> records = result.getRecords().stream()
            .map(po -> convertToLogListResponse(po, apiKeyNameMap, modelInfoMap))
            .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    /**
     * 查询日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    public LogDetailResponse getLogDetail(Long id) {
        RequestLogPO logPO = requestLogMapper.selectById(id);
        if (logPO == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "日志不存在");
        }

        Map<Long, String> apiKeyNameMap = getApiKeyNameMap();
        Map<Long, ModelInfo> modelInfoMap = getModelInfoMap();

        LogDetailResponse response = new LogDetailResponse();
        // 复制基础字段
        LogListResponse baseResponse = convertToLogListResponse(logPO, apiKeyNameMap, modelInfoMap);
        response.setId(baseResponse.getId());
        response.setRequestId(baseResponse.getRequestId());
        response.setApiKeyId(baseResponse.getApiKeyId());
        response.setApiKeyName(baseResponse.getApiKeyName());
        response.setModelId(baseResponse.getModelId());
        response.setModelCode(baseResponse.getModelCode());
        response.setModelType(baseResponse.getModelType());
        response.setRequestTime(baseResponse.getRequestTime());
        response.setResponseTime(baseResponse.getResponseTime());
        response.setDuration(baseResponse.getDuration());
        response.setInputTokens(baseResponse.getInputTokens());
        response.setOutputTokens(baseResponse.getOutputTokens());
        response.setStatus(baseResponse.getStatus());
        response.setErrorMessage(baseResponse.getErrorMessage());
        response.setClientIp(baseResponse.getClientIp());
        response.setUserAgent(baseResponse.getUserAgent());

        // 查询详情数据(请求体/响应体)
        Optional<Map<String, String>> detailOpt = requestLogRepository.findDetailByRequestId(logPO.getRequestId());
        if (detailOpt.isPresent()) {
            Map<String, String> detail = detailOpt.get();
            response.setRequestBody(detail.get("requestBody"));
            response.setResponseBody(detail.get("responseBody"));
        }

        return response;
    }

    /**
     * 转换为日志列表响应对象
     */
    private LogListResponse convertToLogListResponse(RequestLogPO po,
                                                     Map<Long, String> apiKeyNameMap,
                                                     Map<Long, ModelInfo> modelInfoMap) {
        LogListResponse response = new LogListResponse();
        response.setId(po.getId());
        response.setRequestId(po.getRequestId());
        response.setApiKeyId(po.getApiKeyId());
        response.setApiKeyName(apiKeyNameMap.getOrDefault(po.getApiKeyId(), "未知"));
        response.setModelId(po.getModelId());

        ModelInfo modelInfo = modelInfoMap.get(po.getModelId());
        if (modelInfo != null) {
            response.setModelCode(modelInfo.code);
            response.setModelType(modelInfo.type);
        } else {
            response.setModelCode("未知");
            response.setModelType(po.getRequestType());
        }

        response.setRequestTime(po.getCreatedTime());
        // 响应时间 = 请求时间 + 耗时
        if (po.getCreatedTime() != null && po.getResponseTimeMs() != null) {
            response.setResponseTime(po.getCreatedTime().plusNanos(po.getResponseTimeMs() * 1_000_000L));
        }
        response.setDuration(po.getResponseTimeMs());
        response.setInputTokens(po.getPromptTokens());
        response.setOutputTokens(po.getCompletionTokens());
        response.setStatus(po.getStatus() != null && po.getStatus() == 1 ? "SUCCESS" : "FAILED");
        response.setErrorMessage(po.getErrorMessage());
        response.setClientIp(po.getRequestIp());
        response.setUserAgent(po.getUserAgent());

        return response;
    }

    /**
     * 获取API Key名称映射
     */
    private Map<Long, String> getApiKeyNameMap() {
        List<ApiKeyPO> apiKeys = apiKeyMapper.selectList(null);
        return apiKeys.stream()
            .collect(Collectors.toMap(ApiKeyPO::getId, ApiKeyPO::getKeyName, (a, b) -> a));
    }

    /**
     * 获取模型信息映射
     */
    private Map<Long, ModelInfo> getModelInfoMap() {
        List<ModelPO> models = modelMapper.selectList(null);
        return models.stream()
            .collect(Collectors.toMap(
                ModelPO::getId,
                m -> new ModelInfo(m.getModelCode(), m.getModelType()),
                (a, b) -> a
            ));
    }

    /**
     * 模型信息内部类
     */
    private static class ModelInfo {
        String code;
        String type;

        ModelInfo(String code, String type) {
            this.code = code;
            this.type = type;
        }
    }
}
