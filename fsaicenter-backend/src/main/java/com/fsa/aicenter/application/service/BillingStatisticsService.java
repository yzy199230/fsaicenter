package com.fsa.aicenter.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fsa.aicenter.application.dto.response.BillingStatsResponse;
import com.fsa.aicenter.application.dto.response.BillingTrendResponse;
import com.fsa.aicenter.application.dto.response.ModelCostResponse;
import com.fsa.aicenter.infrastructure.persistence.entity.BillingRecordPO;
import com.fsa.aicenter.infrastructure.persistence.entity.ModelPO;
import com.fsa.aicenter.infrastructure.persistence.mapper.BillingRecordMapper;
import com.fsa.aicenter.infrastructure.persistence.mapper.ModelMapper;
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
 * 计费统计服务
 * 提供计费数据的统计分析功能
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingStatisticsService {

    private final BillingRecordMapper billingRecordMapper;
    private final ModelMapper modelMapper;

    /**
     * 获取计费统计数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 计费统计数据
     */
    public BillingStatsResponse getBillingStats(LocalDateTime startTime, LocalDateTime endTime) {
        BillingStatsResponse response = new BillingStatsResponse();

        // 查询时间范围内的计费记录
        LambdaQueryWrapper<BillingRecordPO> query = new LambdaQueryWrapper<>();
        query.between(BillingRecordPO::getBillingTime, startTime, endTime)
                .eq(BillingRecordPO::getIsDeleted, 0);
        List<BillingRecordPO> records = billingRecordMapper.selectList(query);

        // 总请求数
        response.setTotalRequests((long) records.size());

        // 总成本
        BigDecimal totalCost = records.stream()
                .map(BillingRecordPO::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalCost(totalCost);

        // 总使用量（Token数）
        Long totalUsage = records.stream()
                .filter(r -> "TOKEN".equals(r.getBillingType()))
                .map(BillingRecordPO::getUsageAmount)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
        response.setTotalUsage(totalUsage);

        // 平均单价
        if (!records.isEmpty()) {
            BigDecimal avgUnitPrice = records.stream()
                    .map(BillingRecordPO::getUnitPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(records.size()), 6, RoundingMode.HALF_UP);
            response.setAvgUnitPrice(avgUnitPrice);
        } else {
            response.setAvgUnitPrice(BigDecimal.ZERO);
        }

        return response;
    }

    /**
     * 获取计费趋势
     *
     * @param days 天数
     * @return 计费趋势列表
     */
    public List<BillingTrendResponse> getBillingTrend(Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }

        List<BillingTrendResponse> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            // 查询当天的计费记录
            LambdaQueryWrapper<BillingRecordPO> query = new LambdaQueryWrapper<>();
            query.between(BillingRecordPO::getBillingTime, dayStart, dayEnd)
                    .eq(BillingRecordPO::getIsDeleted, 0);
            List<BillingRecordPO> dayRecords = billingRecordMapper.selectList(query);

            BillingTrendResponse trend = new BillingTrendResponse();
            trend.setDate(date.format(formatter));
            trend.setRequests((long) dayRecords.size());

            // 计算当天总成本
            BigDecimal dayCost = dayRecords.stream()
                    .map(BillingRecordPO::getTotalCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            trend.setCost(dayCost);

            result.add(trend);
        }

        return result;
    }

    /**
     * 获取模型成本统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 模型成本统计列表
     */
    public List<ModelCostResponse> getModelCostStats(LocalDateTime startTime, LocalDateTime endTime) {
        // 查询时间范围内的计费记录
        LambdaQueryWrapper<BillingRecordPO> query = new LambdaQueryWrapper<>();
        query.between(BillingRecordPO::getBillingTime, startTime, endTime)
                .eq(BillingRecordPO::getIsDeleted, 0);
        List<BillingRecordPO> records = billingRecordMapper.selectList(query);

        // 按模型ID分组
        Map<Long, List<BillingRecordPO>> groupedByModel = records.stream()
                .filter(r -> r.getModelId() != null)
                .collect(Collectors.groupingBy(BillingRecordPO::getModelId));

        // 获取模型名称映射
        List<ModelPO> models = modelMapper.selectList(null);
        Map<Long, String> modelNameMap = models.stream()
                .collect(Collectors.toMap(ModelPO::getId, ModelPO::getModelName, (a, b) -> a));

        // 构建响应
        List<ModelCostResponse> result = groupedByModel.entrySet().stream()
                .map(entry -> {
                    ModelCostResponse response = new ModelCostResponse();
                    response.setModelId(entry.getKey());
                    response.setModelName(modelNameMap.getOrDefault(entry.getKey(), "未知模型"));
                    response.setRequests((long) entry.getValue().size());

                    // 计算总成本
                    BigDecimal totalCost = entry.getValue().stream()
                            .map(BillingRecordPO::getTotalCost)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    response.setTotalCost(totalCost);

                    // 计算总使用量
                    Long totalUsage = entry.getValue().stream()
                            .map(BillingRecordPO::getUsageAmount)
                            .filter(Objects::nonNull)
                            .reduce(0L, Long::sum);
                    response.setUsage(totalUsage);

                    return response;
                })
                .sorted((a, b) -> b.getTotalCost().compareTo(a.getTotalCost()))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 获取API密钥成本统计
     *
     * @param apiKeyId  API密钥ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 计费统计数据
     */
    public BillingStatsResponse getApiKeyCostStats(Long apiKeyId, LocalDateTime startTime, LocalDateTime endTime) {
        BillingStatsResponse response = new BillingStatsResponse();

        // 查询指定API密钥的计费记录
        LambdaQueryWrapper<BillingRecordPO> query = new LambdaQueryWrapper<>();
        query.eq(BillingRecordPO::getApiKeyId, apiKeyId)
                .between(BillingRecordPO::getBillingTime, startTime, endTime)
                .eq(BillingRecordPO::getIsDeleted, 0);
        List<BillingRecordPO> records = billingRecordMapper.selectList(query);

        // 总请求数
        response.setTotalRequests((long) records.size());

        // 总成本
        BigDecimal totalCost = records.stream()
                .map(BillingRecordPO::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalCost(totalCost);

        // 总使用量（Token数）
        Long totalUsage = records.stream()
                .filter(r -> "TOKEN".equals(r.getBillingType()))
                .map(BillingRecordPO::getUsageAmount)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
        response.setTotalUsage(totalUsage);

        // 平均单价
        if (!records.isEmpty()) {
            BigDecimal avgUnitPrice = records.stream()
                    .map(BillingRecordPO::getUnitPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(records.size()), 6, RoundingMode.HALF_UP);
            response.setAvgUnitPrice(avgUnitPrice);
        } else {
            response.setAvgUnitPrice(BigDecimal.ZERO);
        }

        return response;
    }
}
