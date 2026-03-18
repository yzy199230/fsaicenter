package com.fsa.aicenter.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitLuaScript 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("限流Lua脚本测试")
class RateLimitLuaScriptTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private RateLimitLuaScript rateLimitLuaScript;

    @BeforeEach
    void setUp() {
        rateLimitLuaScript = new RateLimitLuaScript(redisTemplate);
    }

    @Test
    @DisplayName("分钟级限流 - 允许通过")
    void testCheckRateLimitPerMinute_Allowed() {
        // Given
        String key = "ratelimit:apikey:123:minute";
        int limitPerMinute = 100;

        // 模拟Redis返回：允许通过，剩余99次
        List<Long> redisResult = Arrays.asList(1L, 99L);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt(),
                anyLong()
        )).thenReturn(redisResult);

        // When
        RateLimitLuaScript.RateLimitResult result = rateLimitLuaScript.checkRateLimitPerMinute(key, limitPerMinute);

        // Then
        assertTrue(result.allowed());
        assertEquals(100, result.limit());
        assertEquals(99, result.remaining());
        assertTrue(result.resetTime() > 0);
    }

    @Test
    @DisplayName("分钟级限流 - 拒绝通过")
    void testCheckRateLimitPerMinute_Rejected() {
        // Given
        String key = "ratelimit:apikey:123:minute";
        int limitPerMinute = 100;

        // 模拟Redis返回：拒绝，剩余0次
        List<Long> redisResult = Arrays.asList(0L, 0L);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt(),
                anyLong()
        )).thenReturn(redisResult);

        // When
        RateLimitLuaScript.RateLimitResult result = rateLimitLuaScript.checkRateLimitPerMinute(key, limitPerMinute);

        // Then
        assertFalse(result.allowed());
        assertEquals(100, result.limit());
        assertEquals(0, result.remaining());
    }

    @Test
    @DisplayName("天级限流 - 允许通过")
    void testCheckRateLimitPerDay_Allowed() {
        // Given
        String key = "ratelimit:apikey:123:day";
        int limitPerDay = 10000;

        // 模拟Redis返回：允许通过，剩余9999次
        List<Long> redisResult = Arrays.asList(1L, 9999L);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt(),
                anyLong()
        )).thenReturn(redisResult);

        // When
        RateLimitLuaScript.RateLimitResult result = rateLimitLuaScript.checkRateLimitPerDay(key, limitPerDay);

        // Then
        assertTrue(result.allowed());
        assertEquals(10000, result.limit());
        assertEquals(9999, result.remaining());
    }

    @Test
    @DisplayName("天级限流 - 拒绝通过")
    void testCheckRateLimitPerDay_Rejected() {
        // Given
        String key = "ratelimit:apikey:123:day";
        int limitPerDay = 10000;

        // 模拟Redis返回：拒绝，剩余0次
        List<Long> redisResult = Arrays.asList(0L, 0L);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt(),
                anyLong()
        )).thenReturn(redisResult);

        // When
        RateLimitLuaScript.RateLimitResult result = rateLimitLuaScript.checkRateLimitPerDay(key, limitPerDay);

        // Then
        assertFalse(result.allowed());
        assertEquals(10000, result.limit());
        assertEquals(0, result.remaining());
    }

    @Test
    @DisplayName("Redis脚本执行失败 - 返回null")
    void testCheckRateLimit_RedisReturnsNull() {
        // Given
        String key = "ratelimit:apikey:123:minute";
        int limitPerMinute = 100;

        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt(),
                anyLong()
        )).thenReturn(null);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            rateLimitLuaScript.checkRateLimitPerMinute(key, limitPerMinute);
        });
    }

    @Test
    @DisplayName("Redis脚本执行失败 - 返回空列表")
    void testCheckRateLimit_RedisReturnsEmptyList() {
        // Given
        String key = "ratelimit:apikey:123:minute";
        int limitPerMinute = 100;

        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt(),
                anyLong()
        )).thenReturn(Collections.emptyList());

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            rateLimitLuaScript.checkRateLimitPerMinute(key, limitPerMinute);
        });
    }

    @Test
    @DisplayName("验证Redis脚本参数传递正确")
    void testCheckRateLimit_VerifyScriptParameters() {
        // Given
        String key = "ratelimit:apikey:123:minute";
        int limitPerMinute = 100;

        List<Long> redisResult = Arrays.asList(1L, 99L);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt(),
                anyLong()
        )).thenReturn(redisResult);

        // When
        rateLimitLuaScript.checkRateLimitPerMinute(key, limitPerMinute);

        // Then
        verify(redisTemplate, times(1)).execute(
                any(RedisScript.class),
                eq(Collections.singletonList(key)),
                eq(60),           // 窗口大小：60秒
                eq(100),          // 限流阈值：100
                anyLong()         // 当前时间戳
        );
    }
}
