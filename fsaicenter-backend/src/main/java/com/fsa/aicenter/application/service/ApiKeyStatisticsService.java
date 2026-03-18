package com.fsa.aicenter.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fsa.aicenter.application.dto.response.*;
import com.fsa.aicenter.common.model.PageResult;
import com.fsa.aicenter.infrastructure.persistence.entity.BillingRecordPO;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelPO;
import com.fsa.aicenter.infrastructure.persistence.entity.RequestLogPO;
import com.fsa.aicenter.infrastructure.persistence.mapper.BillingRecordMapper;
import com.fsa.aicenter.infrastructure.persistence.mapper.ModelMapper;
import com.fsa.aicenter.infrastructure.persistence.mapper.RequestLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API密钥统计服务
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyStatisticsService {

    private final RequestLogMapper requestLogMapper;
    private final BillingRecordMapper billingRecordMapper;
    private final ModelMapper modelMapper;

    /**
     * 获取API密钥统计数据
     *
     * @param apiKeyId  API密钥ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计数据
     */
    public ApiKeyStatisticsResponse getStatistics(Long apiKeyId, LocalDateTime startTime, LocalDateTime endTime) {
        ApiKeyStatisticsResponse response = new ApiKeyStatisticsResponse();

        // 查询请求日志
        LambdaQueryWrapper<RequestLogPO> logQuery = new LambdaQueryWrapper<>();
        logQuery.eq(RequestLogPO::getApiKeyId, apiKeyId)
                .between(RequestLogPO::getCreatedTime, startTime, endTime)
                .eq(RequestLogPO::getIsDeleted, 0);
        List<RequestLogPO> logs = requestLogMapper.selectList(logQuery);

        // 总请求数
        long totalRequests = logs.size();
        response.setTotalRequests(totalRequests);

        // 成功/失败请求数
        long successRequests = logs.stream().filter(l -> l.getStatus() != null && l.getStatus() == 1).count();
        long failedRequests = totalRequests - successRequests;
        response.setSuccessRequests(successRequests);
        response.setFailedRequests(failedRequests);

        // 成功率
        BigDecimal successRate = totalRequests > 0
                ? BigDecimal.valueOf(successRequests * 100.0 / totalRequests).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        response.setSuccessRate(successRate);

        // 总Token数
        long totalTokens = logs.stream()
                .filter(l -> l.getTotalTokens() != null)
                .mapToLong(RequestLogPO::getTotalTokens)
                .sum();
        response.setTotalTokens(totalTokens);

        // 平均响应时间
        BigDecimal avgResponseTime = totalRequests > 0
                ? BigDecimal.valueOf(logs.stream()
                        .filter(l -> l.getResponseTimeMs() != null)
                        .mapToLong(RequestLogPO::getResponseTimeMs)
                        .average()
                        .orElse(0))
                .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        response.setAvgResponseTime(avgResponseTime);

        // 查询计费记录获取总费用
        LambdaQueryWrapper<BillingRecordPO> billingQuery = new LambdaQueryWrapper<>();
        billingQuery.eq(BillingRecordPO::getApiKeyId, apiKeyId)
                .between(BillingRecordPO::getBillingTime, startTime, endTime)
                .eq(BillingRecordPO::getIsDeleted, 0);
        List<BillingRecordPO> billingRecords = billingRecordMapper.selectList(billingQuery);

        BigDecimal totalCost = billingRecords.stream()
                .map(BillingRecordPO::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalCost(totalCost);

        return response;
    }

    /**
     * 获取使用趋势
     *
     * @param apiKeyId API密钥ID
     * @param days     天数
     * @return 趋势数据列表
     */
    public List<ApiKeyUsageTrendResponse> getUsageTrend(Long apiKeyId, Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }

        List<ApiKeyUsageTrendResponse> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            // 查询当天的日志
            LambdaQueryWrapper<RequestLogPO> query = new LambdaQueryWrapper<>();
            query.eq(RequestLogPO::getApiKeyId, apiKeyId)
                    .between(RequestLogPO::getCreatedTime, dayStart, dayEnd)
                    .eq(RequestLogPO::getIsDeleted, 0);
            List<RequestLogPO> dayLogs = requestLogMapper.selectList(query);

            ApiKeyUsageTrendResponse trend = new ApiKeyUsageTrendResponse();
            trend.setDate(date.format(formatter));
            trend.setRequests((long) dayLogs.size());

            // Token数
            long tokens = dayLogs.stream()
                    .filter(l -> l.getTotalTokens() != null)
                    .mapToLong(RequestLogPO::getTotalTokens)
                    .sum();
            trend.setTokens(tokens);

            // 成功/失败数
            long successCount = dayLogs.stream().filter(l -> l.getStatus() != null && l.getStatus() == 1).count();
            trend.setSuccessCount(successCount);
            trend.setFailedCount((long) dayLogs.size() - successCount);

            result.add(trend);
        }

        return result;
    }

    /**
     * 获取模型分布
     *
     * @param apiKeyId  API密钥ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 模型分布列表
     */
    public List<ApiKeyModelDistributionResponse> getModelDistribution(Long apiKeyId, LocalDateTime startTime, LocalDateTime endTime) {
        // 查询请求日志
        LambdaQueryWrapper<RequestLogPO> query = new LambdaQueryWrapper<>();
        query.eq(RequestLogPO::getApiKeyId, apiKeyId)
                .between(RequestLogPO::getCreatedTime, startTime, endTime)
                .eq(RequestLogPO::getIsDeleted, 0);
        List<RequestLogPO> logs = requestLogMapper.selectList(query);

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        // 按模型ID分组
        Map<Long, List<RequestLogPO>> groupedByModel = logs.stream()
                .filter(l -> l.getModelId() != null)
                .collect(Collectors.groupingBy(RequestLogPO::getModelId));

        // 获取模型信息
        List<ModelPO> models = modelMapper.selectList(null);
        Map<Long, ModelPO> modelMap = models.stream()
                .collect(Collectors.toMap(ModelPO::getId, m -> m, (a, b) -> a));

        long totalRequests = logs.size();

        // 构建响应
        List<ApiKeyModelDistributionResponse> result = groupedByModel.entrySet().stream()
                .map(entry -> {
                    ApiKeyModelDistributionResponse resp = new ApiKeyModelDistributionResponse();
                    resp.setModelId(entry.getKey());

                    ModelPO model = modelMap.get(entry.getKey());
                    if (model != null) {
                        resp.setModelName(model.getModelName());
                        resp.setModelType(model.getModelType());
                    } else {
                        resp.setModelName("未知模型");
                        resp.setModelType("UNKNOWN");
                    }

                    long requests = entry.getValue().size();
                    resp.setRequests(requests);
                    resp.setPercentage(Math.round(requests * 1000.0 / totalRequests) / 10.0);

                    return resp;
                })
                .sorted((a, b) -> Long.compare(b.getRequests(), a.getRequests()))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 分页获取请求日志
     *
     * @param apiKeyId  API密钥ID
     * @param page      页码
     * @param size      每页大小
     * @param status    状态筛选(SUCCESS/FAILED)
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分页日志数据
     */
    public PageResult<ApiKeyLogResponse> getLogs(Long apiKeyId, Integer page, Integer size,
                                                  String status, LocalDateTime startTime, LocalDateTime endTime) {
        // 构建查询条件
        LambdaQueryWrapper<RequestLogPO> query = new LambdaQueryWrapper<>();
        query.eq(RequestLogPO::getApiKeyId, apiKeyId)
                .eq(RequestLogPO::getIsDeleted, 0);

        if (startTime != null && endTime != null) {
            query.between(RequestLogPO::getCreatedTime, startTime, endTime);
        }

        if ("SUCCESS".equalsIgnoreCase(status)) {
            query.eq(RequestLogPO::getStatus, 1);
        } else if ("FAILED".equalsIgnoreCase(status)) {
            query.eq(RequestLogPO::getStatus, 0);
        }

        query.orderByDesc(RequestLogPO::getCreatedTime);

        // 分页查询
        Page<RequestLogPO> pageParam = new Page<>(page, size);
        IPage<RequestLogPO> pageResult = requestLogMapper.selectPage(pageParam, query);

        // 获取模型信息映射
        List<ModelPO> models = modelMapper.selectList(null);
        Map<Long, ModelPO> modelMap = models.stream()
                .collect(Collectors.toMap(ModelPO::getId, m -> m, (a, b) -> a));

        // 转换结果
        List<ApiKeyLogResponse> list = pageResult.getRecords().stream()
                .map(log -> {
                    ApiKeyLogResponse resp = new ApiKeyLogResponse();
                    resp.setId(log.getId());
                    resp.setRequestId(log.getRequestId());

                    ModelPO model = modelMap.get(log.getModelId());
                    if (model != null) {
                        resp.setModelName(model.getModelName());
                        resp.setModelType(model.getModelType());
                    } else {
                        resp.setModelName("未知模型");
                        resp.setModelType("UNKNOWN");
                    }

                    resp.setStatus(log.getStatus() != null && log.getStatus() == 1 ? "SUCCESS" : "FAILED");
                    resp.setPromptTokens(log.getPromptTokens() != null ? log.getPromptTokens().longValue() : 0L);
                    resp.setCompletionTokens(log.getCompletionTokens() != null ? log.getCompletionTokens().longValue() : 0L);
                    resp.setTotalTokens(log.getTotalTokens() != null ? log.getTotalTokens().longValue() : 0L);
                    resp.setResponseTime(log.getResponseTimeMs() != null ? log.getResponseTimeMs().longValue() : 0L);
                    resp.setErrorMessage(log.getErrorMessage());
                    resp.setRequestTime(log.getCreatedTime());

                    return resp;
                })
                .collect(Collectors.toList());

        return PageResult.of(pageResult.getTotal(), page, size, list);
    }
}
