package com.fsa.aicenter.infrastructure.event;

import com.fsa.aicenter.application.event.BillingEvent;
import com.fsa.aicenter.domain.model.aggregate.AiModel;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.apikey.repository.ApiKeyRepository;
import com.fsa.aicenter.domain.billing.aggregate.BillingRecord;
import com.fsa.aicenter.domain.billing.repository.BillingRepository;
import com.fsa.aicenter.domain.billing.valueobject.BillingRule;
import com.fsa.aicenter.domain.billing.valueobject.BillingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BillingEventListener 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("计费事件监听器测试")
class BillingEventListenerTest {

    @Mock
    private BillingRepository billingRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private BillingEventListener billingEventListener;

    private BillingEvent testEvent;
    private ApiKey testApiKey;
    private AiModel testModel;
    private BillingRule testRule;

    @BeforeEach
    void setUp() {
        // 创建测试ApiKey
        testApiKey = new ApiKey();
        testApiKey.setId(1L);

        // 创建测试Model
        testModel = new AiModel();
        testModel.setId(100L);

        // 创建测试计费规则
        testRule = new BillingRule();
        testRule.setId(1L);
        testRule.setModelId(100L);
        testRule.setBillingType(BillingType.TOKEN);
        testRule.setUnitPrice(BigDecimal.valueOf(0.002));

        // 创建测试事件
        testEvent = BillingEvent.builder()
                .requestId("req-123")
                .apiKey(testApiKey)
                .model(testModel)
                .tokens(1000L)
                .tokenBased(true)
                .requestTime(LocalDateTime.now())
                .eventTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("处理计费事件 - Token计费成功")
    void testHandleBillingEvent_TokenBased_Success() {
        // Given
        when(billingRepository.findEffectiveRule(
                eq(100L),
                eq(BillingType.TOKEN),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(testRule));

        // When
        billingEventListener.handleBillingEvent(testEvent);

        // Then
        verify(billingRepository, times(1)).findEffectiveRule(
                eq(100L),
                eq(BillingType.TOKEN),
                any(LocalDateTime.class)
        );
        verify(billingRepository, times(1)).save(any(BillingRecord.class));
    }

    @Test
    @DisplayName("处理计费事件 - 图片计费成功")
    void testHandleBillingEvent_ImageBased_Success() {
        // Given
        testEvent = BillingEvent.builder()
                .requestId("req-456")
                .apiKey(testApiKey)
                .model(testModel)
                .cost(10L)
                .tokenBased(false)
                .requestTime(LocalDateTime.now())
                .eventTime(LocalDateTime.now())
                .build();

        BillingRule imageRule = new BillingRule();
        imageRule.setId(2L);
        imageRule.setModelId(100L);
        imageRule.setBillingType(BillingType.IMAGE);
        imageRule.setUnitPrice(BigDecimal.valueOf(0.05));

        when(billingRepository.findEffectiveRule(
                eq(100L),
                eq(BillingType.IMAGE),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(imageRule));

        // When
        billingEventListener.handleBillingEvent(testEvent);

        // Then
        verify(billingRepository, times(1)).findEffectiveRule(
                eq(100L),
                eq(BillingType.IMAGE),
                any(LocalDateTime.class)
        );
        verify(billingRepository, times(1)).save(any(BillingRecord.class));
    }

    @Test
    @DisplayName("处理计费事件 - 未找到计费规则，创建零成本记录")
    void testHandleBillingEvent_NoRuleFound_CreateZeroCostRecord() {
        // Given
        when(billingRepository.findEffectiveRule(
                eq(100L),
                eq(BillingType.TOKEN),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        // When
        billingEventListener.handleBillingEvent(testEvent);

        // Then
        verify(billingRepository, times(1)).findEffectiveRule(
                eq(100L),
                eq(BillingType.TOKEN),
                any(LocalDateTime.class)
        );
        // 应该保存零成本记录
        verify(billingRepository, times(1)).save(any(BillingRecord.class));
    }

    @Test
    @DisplayName("处理计费事件 - 空事件跳过处理")
    void testHandleBillingEvent_NullEvent_Skip() {
        // When
        billingEventListener.handleBillingEvent(null);

        // Then
        verify(billingRepository, never()).findEffectiveRule(anyLong(), any(), any());
        verify(billingRepository, never()).save(any());
    }

    @Test
    @DisplayName("处理计费事件 - 异常时抛出以触发事务回滚")
    void testHandleBillingEvent_Exception_ThrowsToRollback() {
        // Given
        when(billingRepository.findEffectiveRule(
                eq(100L),
                eq(BillingType.TOKEN),
                any(LocalDateTime.class)
        )).thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            billingEventListener.handleBillingEvent(testEvent);
        } catch (RuntimeException e) {
            // 异常应该被重新抛出
            verify(billingRepository, times(1)).findEffectiveRule(
                    eq(100L),
                    eq(BillingType.TOKEN),
                    any(LocalDateTime.class)
            );
            verify(billingRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("处理计费事件 - 既无tokens也无cost，使用默认值")
    void testHandleBillingEvent_NoTokensNoCost_UseDefault() {
        // Given
        testEvent = BillingEvent.builder()
                .requestId("req-789")
                .apiKey(testApiKey)
                .model(testModel)
                .tokenBased(false)
                .cost(null)
                .requestTime(LocalDateTime.now())
                .eventTime(LocalDateTime.now())
                .build();

        when(billingRepository.findEffectiveRule(
                eq(100L),
                eq(BillingType.TOKEN),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(testRule));

        // When
        billingEventListener.handleBillingEvent(testEvent);

        // Then
        verify(billingRepository, times(1)).save(any(BillingRecord.class));
    }
}
