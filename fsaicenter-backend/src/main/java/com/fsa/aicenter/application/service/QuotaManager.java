package com.fsa.aicenter.application.service;

import com.fsa.aicenter.common.exception.QuotaExceededException;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.apikey.repository.ApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 配额管理服务
 * <p>
 * 实现��额的三阶段管理：预扣 -> 确认 -> 回滚
 * 使用Redis存储预扣记录，支持处理估算与实际消耗的差额。
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Service
public class QuotaManager {

    private static final String QUOTA_PREDEDUCT_KEY_PREFIX = "quota:prededuct:";
    private static final long PREDEDUCT_TTL_MINUTES = 30;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    /**
     * 预扣配额
     * <p>
     * 1. 检查配额是否足够
     * 2. 预扣估算的配额
     * 3. 保存预扣记录到Redis（30分钟过期）
     * 4. 持久化ApiKey状态
     * </p>
     *
     * @param apiKey         API密钥对象
     * @param estimatedTokens 估算的token数量
     * @return 预扣记录ID，用于后续确认或回滚
     * @throws QuotaExceededException 配额不足异常
     */
    @Transactional(rollbackFor = Exception.class)
    public String preDeduct(ApiKey apiKey, long estimatedTokens) {
        if (apiKey == null) {
            throw new IllegalArgumentException("ApiKey cannot be null");
        }
        if (estimatedTokens < 0) {
            throw new IllegalArgumentException("Estimated tokens cannot be negative: " + estimatedTokens);
        }

        log.debug("预扣配额: apiKeyId={}, estimatedTokens={}", apiKey.getId(), estimatedTokens);

        // 检查配额是否足��
        if (!apiKey.hasQuota(estimatedTokens)) {
            long available = apiKey.getQuota() != null ? apiKey.getQuota().remaining() : 0;
            log.warn("配额不足: apiKeyId={}, required={}, available={}",
                     apiKey.getId(), estimatedTokens, available);
            throw new QuotaExceededException(estimatedTokens, available);
        }

        // 预扣配额
        apiKey.consumeQuota(estimatedTokens);

        // 持久化ApiKey状态
        apiKeyRepository.update(apiKey);

        // 生成预扣记录ID
        String preDeductId = UUID.randomUUID().toString();
        String redisKey = QUOTA_PREDEDUCT_KEY_PREFIX + preDeductId;

        // 保存预扣记录到Redis
        PreDeductRecord record = new PreDeductRecord(apiKey.getId(), estimatedTokens);
        redisTemplate.opsForValue().set(redisKey, record, PREDEDUCT_TTL_MINUTES, TimeUnit.MINUTES);

        log.info("配额预扣成功: preDeductId={}, apiKeyId={}, estimatedTokens={}",
                 preDeductId, apiKey.getId(), estimatedTokens);

        return preDeductId;
    }

    /**
     * 确认扣减
     * <p>
     * 1. 从Redis获取预扣记录
     * 2. 计算实际消耗与估算的差额
     * 3. 根据差额调整配额（多退少补）
     * 4. 删除预扣记录
     * </p>
     *
     * @param preDeductId  预扣记录ID
     * @param actualTokens 实际消耗的token数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(String preDeductId, long actualTokens) {
        if (preDeductId == null || preDeductId.isEmpty()) {
            log.warn("预扣记录ID为空，跳过确认");
            return;
        }
        if (actualTokens < 0) {
            throw new IllegalArgumentException("Actual tokens cannot be negative: " + actualTokens);
        }

        String redisKey = QUOTA_PREDEDUCT_KEY_PREFIX + preDeductId;
        PreDeductRecord record = (PreDeductRecord) redisTemplate.opsForValue().get(redisKey);

        if (record == null) {
            log.warn("预扣记录不存在或已过期: preDeductId={}", preDeductId);
            return;
        }

        log.debug("确认配额扣减: preDeductId={}, apiKeyId={}, estimated={}, actual={}",
                  preDeductId, record.getApiKeyId(), record.getEstimatedTokens(), actualTokens);

        // 计算差额
        long diff = actualTokens - record.getEstimatedTokens();

        if (diff != 0) {
            // 获取ApiKey
            ApiKey apiKey = apiKeyRepository.findById(record.getApiKeyId())
                    .orElseThrow(() -> new IllegalStateException("ApiKey not found: " + record.getApiKeyId()));

            if (diff > 0) {
                // 实际消耗更多，继续扣减差额
                log.debug("实际消耗大于估算，补扣差额: diff={}", diff);
                if (!apiKey.hasQuota(diff)) {
                    log.warn("补扣配额不足，但请求已完成，仅记录: apiKeyId={}, diff={}",
                             apiKey.getId(), diff);
                    // 这里可以选择强制扣减或记录欠费，根据业务需求决定
                    // 为了避免请求已成功但扣费失败的情况，这里选择强制扣减
                    apiKey.consumeQuota(diff);
                } else {
                    apiKey.consumeQuota(diff);
                }
            } else {
                // 实际消耗更少，退回差额
                log.debug("实际消耗小于估算，退回差额: diff={}", -diff);
                apiKey.refundQuota(-diff);
            }

            // 持久化ApiKey状态
            apiKeyRepository.update(apiKey);
        }

        // 删除预扣记录
        redisTemplate.delete(redisKey);

        log.info("配额确认成功: preDeductId={}, apiKeyId={}, actualTokens={}",
                 preDeductId, record.getApiKeyId(), actualTokens);
    }

    /**
     * 回滚配额
     * <p>
     * 1. 从Redis获取预扣记录
     * 2. 退回预扣的配额
     * 3. 删除预扣记录
     * </p>
     *
     * @param preDeductId 预扣记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollback(String preDeductId) {
        if (preDeductId == null || preDeductId.isEmpty()) {
            log.warn("预扣记录ID为空，跳过回滚");
            return;
        }

        String redisKey = QUOTA_PREDEDUCT_KEY_PREFIX + preDeductId;
        PreDeductRecord record = (PreDeductRecord) redisTemplate.opsForValue().get(redisKey);

        if (record == null) {
            log.warn("预扣记录不存在或已过期: preDeductId={}", preDeductId);
            return;
        }

        log.debug("回滚配额: preDeductId={}, apiKeyId={}, estimatedTokens={}",
                  preDeductId, record.getApiKeyId(), record.getEstimatedTokens());

        // 获取ApiKey
        ApiKey apiKey = apiKeyRepository.findById(record.getApiKeyId())
                .orElseThrow(() -> new IllegalStateException("ApiKey not found: " + record.getApiKeyId()));

        // 退回预扣的配额
        apiKey.refundQuota(record.getEstimatedTokens());

        // 持久化ApiKey状态
        apiKeyRepository.update(apiKey);

        // 删除预扣记录
        redisTemplate.delete(redisKey);

        log.info("配额回滚成功: preDeductId={}, apiKeyId={}, refundedTokens={}",
                 preDeductId, record.getApiKeyId(), record.getEstimatedTokens());
    }

    /**
     * 预扣记录（存储在Redis中）
     */
    public static class PreDeductRecord implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private Long apiKeyId;
        private Long estimatedTokens;

        public PreDeductRecord() {
        }

        public PreDeductRecord(Long apiKeyId, Long estimatedTokens) {
            this.apiKeyId = apiKeyId;
            this.estimatedTokens = estimatedTokens;
        }

        public Long getApiKeyId() {
            return apiKeyId;
        }

        public void setApiKeyId(Long apiKeyId) {
            this.apiKeyId = apiKeyId;
        }

        public Long getEstimatedTokens() {
            return estimatedTokens;
        }

        public void setEstimatedTokens(Long estimatedTokens) {
            this.estimatedTokens = estimatedTokens;
        }
    }
}
