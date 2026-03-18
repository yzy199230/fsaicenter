package com.fsa.aicenter.application.service;

import com.fsa.aicenter.application.dto.response.*;
import com.fsa.aicenter.infrastructure.persistence.mapper.*;
import com.fsa.aicenter.infrastructure.persistence.entity.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 * 仪表板服务
 * 提供仪表板统计数据查询功能
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RequestLogMapper requestLogMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final ModelMapper modelMapper;
    private final BillingRecordMapper billingRecordMapper;

    /**
     * 获取仪表板统计数据
     */
    public DashboardStatsResponse getStats() {
        DashboardStatsResponse response = new DashboardStatsResponse();

        // 今日开始和结束时间
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        // 今日请求数
        LambdaQueryWrapper<RequestLogPO> todayQuery = new LambdaQueryWrapper<>();
        todayQuery.between(RequestLogPO::getCreatedTime, todayStart, todayEnd);
        Long todayTotal = requestLogMapper.selectCount(todayQuery);
        response.setTodayRequests(todayTotal);

        // 今日成功数
        LambdaQueryWrapper<RequestLogPO> successQuery = new LambdaQueryWrapper<>();
        successQuery.between(RequestLogPO::getCreatedTime, todayStart, todayEnd)
                .eq(RequestLogPO::getStatus, 1);
        Long todaySuccess = requestLogMapper.selectCount(successQuery);

        // 成功率
        BigDecimal successRate = todayTotal > 0
                ? BigDecimal.valueOf(todaySuccess).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(todayTotal), 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100);
        response.setSuccessRate(successRate);

        // 活跃API密钥数(状态=1的)
        LambdaQueryWrapper<ApiKeyPO> activeKeyQuery = new LambdaQueryWrapper<>();
        activeKeyQuery.eq(ApiKeyPO::getStatus, 1);
        Long activeKeys = apiKeyMapper.selectCount(activeKeyQuery);
        response.setActiveApiKeys(activeKeys.intValue());

        // 总成本 - 查询今日计费记录并聚合
        LambdaQueryWrapper<BillingRecordPO> billingQuery = new LambdaQueryWrapper<>();
        billingQuery.between(BillingRecordPO::getBillingTime, todayStart, todayEnd)
                .eq(BillingRecordPO::getIsDeleted, 0);
        List<BillingRecordPO> billingRecords = billingRecordMapper.selectList(billingQuery);

        // 使用Stream API聚合总成本
        BigDecimal totalCost = billingRecords.stream()
                .map(BillingRecordPO::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setTotalCost(totalCost);

        return response;
    }

    /**
     * 获取请求趋势
     *
     * @param days 天数，默认7天
     */
    public DashboardTrendResponse getRequestTrend(Integer days) {
        DashboardTrendResponse response = new DashboardTrendResponse();

        if (days == null || days <= 0) {
            days = 7;
        }

        List<String> xAxis = new ArrayList<>();
        List<Long> totalData = new ArrayList<>();
        List<Long> successData = new ArrayList<>();
        List<Long> failedData = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            xAxis.add(date.format(formatter));

            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            // 当天总数
            LambdaQueryWrapper<RequestLogPO> totalQuery = new LambdaQueryWrapper<>();
            totalQuery.between(RequestLogPO::getCreatedTime, dayStart, dayEnd);
            Long total = requestLogMapper.selectCount(totalQuery);
            totalData.add(total);

            // 当天成功数
            LambdaQueryWrapper<RequestLogPO> successQuery = new LambdaQueryWrapper<>();
            successQuery.between(RequestLogPO::getCreatedTime, dayStart, dayEnd)
                    .eq(RequestLogPO::getStatus, 1);
            Long success = requestLogMapper.selectCount(successQuery);
            successData.add(success);

            // 当天失败数
            failedData.add(total - success);
        }

        response.setXAxis(xAxis);

        List<DashboardTrendResponse.SeriesData> series = new ArrayList<>();

        DashboardTrendResponse.SeriesData totalSeries = new DashboardTrendResponse.SeriesData();
        totalSeries.setName("总请求");
        totalSeries.setData(totalData);
        series.add(totalSeries);

        DashboardTrendResponse.SeriesData successSeries = new DashboardTrendResponse.SeriesData();
        successSeries.setName("成功");
        successSeries.setData(successData);
        series.add(successSeries);

        DashboardTrendResponse.SeriesData failedSeries = new DashboardTrendResponse.SeriesData();
        failedSeries.setName("失败");
        failedSeries.setData(failedData);
        series.add(failedSeries);

        response.setSeries(series);

        return response;
    }

    /**
     * 获取模型使用分布
     */
    public List<ModelDistributionResponse> getModelDistribution() {
        // 查询所有日志，按modelId分组统计
        LambdaQueryWrapper<RequestLogPO> query = new LambdaQueryWrapper<>();
        query.select(RequestLogPO::getModelId);
        List<RequestLogPO> logs = requestLogMapper.selectList(query);

        // 按modelId分组计数
        Map<Long, Long> countMap = logs.stream()
                .filter(log -> log.getModelId() != null)
                .collect(Collectors.groupingBy(RequestLogPO::getModelId, Collectors.counting()));

        // 获取模型名称映射
        List<ModelPO> models = modelMapper.selectList(null);
        Map<Long, String> modelNameMap = models.stream()
                .collect(Collectors.toMap(ModelPO::getId, ModelPO::getModelName, (a, b) -> a));

        // 构建响应
        return countMap.entrySet().stream()
                .map(entry -> {
                    ModelDistributionResponse resp = new ModelDistributionResponse();
                    resp.setName(modelNameMap.getOrDefault(entry.getKey(), "未知模型"));
                    resp.setValue(entry.getValue());
                    return resp;
                })
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取热门模型 Top N
     *
     * @param limit 数量限制，默认10
     */
    public List<TopModelResponse> getTopModels(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<ModelDistributionResponse> distribution = getModelDistribution();

        return distribution.stream()
                .limit(limit)
                .map(dist -> {
                    TopModelResponse resp = new TopModelResponse();
                    resp.setModelName(dist.getName());
                    resp.setCallCount(dist.getValue());
                    return resp;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取最近日志
     *
     * @param limit 数量限制，默认10
     */
    public List<RecentLogResponse> getRecentLogs(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        LambdaQueryWrapper<RequestLogPO> query = new LambdaQueryWrapper<>();
        query.orderByDesc(RequestLogPO::getCreatedTime)
                .last("LIMIT " + limit);
        List<RequestLogPO> logs = requestLogMapper.selectList(query);

        // 获取模型名称映射
        List<ModelPO> models = modelMapper.selectList(null);
        Map<Long, String> modelNameMap = models.stream()
                .collect(Collectors.toMap(ModelPO::getId, ModelPO::getModelName, (a, b) -> a));

        // 获取ApiKey名称映射
        List<ApiKeyPO> apiKeys = apiKeyMapper.selectList(null);
        Map<Long, String> apiKeyNameMap = apiKeys.stream()
                .collect(Collectors.toMap(ApiKeyPO::getId, ApiKeyPO::getKeyName, (a, b) -> a));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return logs.stream()
                .map(log -> {
                    RecentLogResponse resp = new RecentLogResponse();
                    resp.setTime(log.getCreatedTime() != null ? log.getCreatedTime().format(formatter) : "");
                    resp.setModel(modelNameMap.getOrDefault(log.getModelId(), "未知"));
                    resp.setApiKey(apiKeyNameMap.getOrDefault(log.getApiKeyId(), "未知"));
                    resp.setStatus(log.getStatus());
                    resp.setDuration(log.getResponseTimeMs() != null ? log.getResponseTimeMs() + "ms" : "-");
                    return resp;
                })
                .collect(Collectors.toList());
    }
}
