package com.fsa.aicenter.infrastructure.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 限流Lua脚本
 * 使用Redis滑动窗口算法实现限流
 *
 * @author FSA AI Center
 */
@Component
public class RateLimitLuaScript {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 滑动窗口限流脚本
     * KEYS[1]: 限流key
     * ARGV[1]: 窗口大小（秒）
     * ARGV[2]: 限流阈值
     * ARGV[3]: 当前时间戳（毫秒）
     * 返回值: [是否允许通过(1/0), 剩余请求数]
     */
    private static final String SLIDING_WINDOW_SCRIPT =
            """
            local key = KEYS[1]
            local window_size = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])

            -- 窗口开始时间
            local window_start = current_time - window_size * 1000

            -- 删除过期记录
            redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

            -- 获取当前窗口内的请求数
            local current_count = redis.call('ZCARD', key)

            -- 检查是否超过限流阈值
            if current_count < limit then
                -- 添加当前请求
                redis.call('ZADD', key, current_time, current_time)
                -- 设置过期时间（窗口大小 + 1秒的缓冲）
                redis.call('EXPIRE', key, window_size + 1)
                -- 返回 [允许通过, 剩余请求数]
                return {1, limit - current_count - 1}
            else
                -- 返回 [拒绝, 0]
                return {0, 0}
            end
            """;

    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> slidingWindowScript;

    public RateLimitLuaScript(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;

        // 初始化脚本
        this.slidingWindowScript = new DefaultRedisScript<>();
        this.slidingWindowScript.setScriptText(SLIDING_WINDOW_SCRIPT);
        this.slidingWindowScript.setResultType(List.class);
    }

    /**
     * 检查是否允许请求通过（分钟级限流）
     *
     * @param key           限流key（例如：ratelimit:apikey:123:minute）
     * @param limitPerMinute 每分钟限流阈值
     * @return RateLimitResult 限流结果
     */
    public RateLimitResult checkRateLimitPerMinute(String key, int limitPerMinute) {
        return checkRateLimit(key, 60, limitPerMinute);
    }

    /**
     * 检查是否允许请求通过（天级限流）
     *
     * @param key        限流key（例如：ratelimit:apikey:123:day）
     * @param limitPerDay 每天限流阈值
     * @return RateLimitResult 限流结果
     */
    public RateLimitResult checkRateLimitPerDay(String key, int limitPerDay) {
        return checkRateLimit(key, 86400, limitPerDay);
    }

    /**
     * 检查是否允许请求通过
     *
     * @param key        限流key
     * @param windowSize 窗口大小（秒）
     * @param limit      限流阈值
     * @return RateLimitResult 限流结果
     */
    @SuppressWarnings("unchecked")
    private RateLimitResult checkRateLimit(String key, int windowSize, int limit) {
        long currentTime = Instant.now().toEpochMilli();

        List<Long> result = redisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList(key),
                windowSize,
                limit,
                currentTime
        );

        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Redis Lua script execution failed");
        }

        boolean allowed = result.get(0) == 1;
        long remaining = result.get(1);

        // 计算重置时间（窗口结束时间）
        long resetTime = (currentTime + windowSize * 1000L) / 1000L;

        return new RateLimitResult(allowed, limit, remaining, resetTime);
    }

    /**
     * 限流结果
     *
     * @param allowed   是否允许通过
     * @param limit     限流阈值
     * @param remaining 剩余请求数
     * @param resetTime 重置时间（Unix时间戳，秒）
     */
    public record RateLimitResult(boolean allowed, long limit, long remaining, long resetTime) {
    }
}
