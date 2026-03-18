package com.fsa.aicenter.application.service;

import com.fsa.aicenter.common.exception.QuotaExceededException;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.apikey.repository.ApiKeyRepository;
import com.fsa.aicenter.domain.apikey.valueobject.Quota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QuotaManager 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("配额管理服务测试")
class QuotaManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private QuotaManager quotaManager;

    private ApiKey testApiKey;

    @BeforeEach
    void setUp() {
        // 模拟 RedisTemplate 的 opsForValue() 方法
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 创建测试用的 ApiKey
        testApiKey = createTestApiKey(1L, 10000L, 0L);
    }

    @Test
    @DisplayName("预扣配额 - 成功场景")
    void testPreDeduct_Success() {
        // Given
        long estimatedTokens = 1000L;

        // When
        String preDeductId = quotaManager.preDeduct(testApiKey, estimatedTokens);

        // Then
        assertNotNull(preDeductId);
        verify(apiKeyRepository, times(1)).update(testApiKey);
        verify(valueOperations, times(1)).set(
                startsWith("quota:prededuct:"),
                any(QuotaManager.PreDeductRecord.class),
                eq(30L),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    @DisplayName("预扣配额 - 配额不足抛出异常")
    void testPreDeduct_QuotaExceeded() {
        // Given
        long estimatedTokens = 20000L; // 超过可用配额

        // When & Then
        assertThrows(QuotaExceededException.class, () -> {
            quotaManager.preDeduct(testApiKey, estimatedTokens);
        });

        // 验证没有调用持久化
        verify(apiKeyRepository, never()).update(any());
    }

    @Test
    @DisplayName("预扣配额 - ApiKey为null抛出异常")
    void testPreDeduct_NullApiKey() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            quotaManager.preDeduct(null, 1000L);
        });
    }

    @Test
    @DisplayName("预扣配额 - 负数Token抛出异常")
    void testPreDeduct_NegativeTokens() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            quotaManager.preDeduct(testApiKey, -100L);
        });
    }

    @Test
    @DisplayName("确认扣减 - 实际消耗等于估算")
    void testConfirm_ActualEqualsEstimated() {
        // Given
        String preDeductId = "test-prededuct-id";
        long estimatedTokens = 1000L;
        long actualTokens = 1000L;

        QuotaManager.PreDeductRecord record = new QuotaManager.PreDeductRecord(1L, estimatedTokens);
        when(valueOperations.get("quota:prededuct:" + preDeductId)).thenReturn(record);

        // When
        quotaManager.confirm(preDeductId, actualTokens);

        // Then
        verify(redisTemplate, times(1)).delete("quota:prededuct:" + preDeductId);
        // 差额为0，不应该调用update
        verify(apiKeyRepository, never()).update(any());
    }

    @Test
    @DisplayName("确认扣减 - 实际消耗大于估算（补扣）")
    void testConfirm_ActualGreaterThanEstimated() {
        // Given
        String preDeductId = "test-prededuct-id";
        long estimatedTokens = 1000L;
        long actualTokens = 1500L;

        QuotaManager.PreDeductRecord record = new QuotaManager.PreDeductRecord(1L, estimatedTokens);
        when(valueOperations.get("quota:prededuct:" + preDeductId)).thenReturn(record);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));

        // When
        quotaManager.confirm(preDeductId, actualTokens);

        // Then
        verify(apiKeyRepository, times(1)).update(testApiKey);
        verify(redisTemplate, times(1)).delete("quota:prededuct:" + preDeductId);
    }

    @Test
    @DisplayName("确认扣减 - 实际消耗小于估算（退回）")
    void testConfirm_ActualLessThanEstimated() {
        // Given
        String preDeductId = "test-prededuct-id";
        long estimatedTokens = 1000L;
        long actualTokens = 500L;

        QuotaManager.PreDeductRecord record = new QuotaManager.PreDeductRecord(1L, estimatedTokens);
        when(valueOperations.get("quota:prededuct:" + preDeductId)).thenReturn(record);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));

        // When
        quotaManager.confirm(preDeductId, actualTokens);

        // Then
        verify(apiKeyRepository, times(1)).update(testApiKey);
        verify(redisTemplate, times(1)).delete("quota:prededuct:" + preDeductId);
    }

    @Test
    @DisplayName("确认扣减 - 预扣记录不存在")
    void testConfirm_RecordNotFound() {
        // Given
        String preDeductId = "non-existent-id";
        when(valueOperations.get("quota:prededuct:" + preDeductId)).thenReturn(null);

        // When
        quotaManager.confirm(preDeductId, 1000L);

        // Then
        verify(apiKeyRepository, never()).update(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("回滚配额 - 成功场景")
    void testRollback_Success() {
        // Given
        String preDeductId = "test-prededuct-id";
        long estimatedTokens = 1000L;

        QuotaManager.PreDeductRecord record = new QuotaManager.PreDeductRecord(1L, estimatedTokens);
        when(valueOperations.get("quota:prededuct:" + preDeductId)).thenReturn(record);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));

        // When
        quotaManager.rollback(preDeductId);

        // Then
        verify(apiKeyRepository, times(1)).update(testApiKey);
        verify(redisTemplate, times(1)).delete("quota:prededuct:" + preDeductId);
    }

    @Test
    @DisplayName("回滚配额 - 预扣记录不存在")
    void testRollback_RecordNotFound() {
        // Given
        String preDeductId = "non-existent-id";
        when(valueOperations.get("quota:prededuct:" + preDeductId)).thenReturn(null);

        // When
        quotaManager.rollback(preDeductId);

        // Then
        verify(apiKeyRepository, never()).update(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("回滚配额 - preDeductId为空")
    void testRollback_EmptyPreDeductId() {
        // When
        quotaManager.rollback("");

        // Then
        verify(valueOperations, never()).get(anyString());
        verify(apiKeyRepository, never()).update(any());
    }

    /**
     * 创建测试用的 ApiKey
     */
    private ApiKey createTestApiKey(Long id, Long totalQuota, Long usedQuota) {
        // 注意：这里需要根据实际的 ApiKey 构造方法调整
        // 假设 ApiKey 有一个构造方法或 builder
        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setQuota(new Quota(totalQuota, usedQuota));
        apiKey.enable();
        return apiKey;
    }
}
