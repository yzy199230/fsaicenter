package com.fsa.aicenter.interfaces.filter;

import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.common.exception.RateLimitException;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.apikey.valueobject.RateLimit;
import com.fsa.aicenter.infrastructure.ratelimit.RateLimitLuaScript;
import com.fsa.aicenter.infrastructure.ratelimit.RateLimitLuaScript.RateLimitResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 限流过滤器
 * 基于API Key进行限流，使用Redis滑动窗口算法
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /**
     * 限流Key前缀
     */
    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:apikey:";

    /**
     * 响应头：限流阈值
     */
    private static final String HEADER_RATE_LIMIT = "X-RateLimit-Limit";

    /**
     * 响应头：剩余请求数
     */
    private static final String HEADER_RATE_REMAINING = "X-RateLimit-Remaining";

    /**
     * 响应头：重置时间（Unix时间戳）
     */
    private static final String HEADER_RATE_RESET = "X-RateLimit-Reset";

    private final RateLimitLuaScript rateLimitLuaScript;

    public RateLimitFilter(RateLimitLuaScript rateLimitLuaScript) {
        this.rateLimitLuaScript = rateLimitLuaScript;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 获取API Key对象（由ApiKeyAuthenticationFilter放入）
        ApiKey apiKey = (ApiKey) request.getAttribute(ApiKeyAuthenticationFilter.API_KEY_ATTRIBUTE);

        if (apiKey == null) {
            log.error("API Key not found in request attributes. ApiKeyAuthenticationFilter may not be executed.");
            throw new IllegalStateException("API Key not found in request");
        }

        // 2. 获取限流配置
        RateLimit rateLimit = apiKey.getRateLimit();

        if (rateLimit == null) {
            // 没有配置限流，直接放行
            log.debug("No rate limit configured for API Key: {}", apiKey.getId());
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 检查分钟级限流
        if (rateLimit.hasMinuteLimit()) {
            checkRateLimitPerMinute(apiKey, rateLimit, response);
        }

        // 4. 检查天级限流
        if (rateLimit.hasDayLimit()) {
            checkRateLimitPerDay(apiKey, rateLimit, response);
        }

        // 5. 继续过滤链
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 与ApiKeyAuthenticationFilter保持一致，排除不需要认证的路径
        String path = request.getRequestURI();
        return path.startsWith("/admin/") ||  // 管理后台接口（使用sa-token认证）
               path.startsWith("/doc.html") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-resources") ||
               path.startsWith("/webjars/") ||
               path.startsWith("/favicon.ico") ||
               path.startsWith("/error");
    }

    /**
     * 检查分钟级限流
     *
     * @param apiKey    API Key对象
     * @param rateLimit 限流配置
     * @param response  HTTP响应
     * @throws RateLimitException 如果超过限流阈值
     */
    private void checkRateLimitPerMinute(ApiKey apiKey, RateLimit rateLimit, HttpServletResponse response) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey.getId() + ":minute";
        int limit = rateLimit.getPerMinute();

        RateLimitResult result = rateLimitLuaScript.checkRateLimitPerMinute(key, limit);

        // 设置响应头
        setRateLimitHeaders(response, result);

        if (!result.allowed()) {
            log.warn("Rate limit exceeded (per minute): apiKeyId={}, limit={}", apiKey.getId(), limit);
            throw new RateLimitException(ErrorCode.API_KEY_RATE_LIMIT_EXCEEDED,
                    "Rate limit exceeded: " + limit + " requests per minute");
        }

        log.debug("Rate limit check passed (per minute): apiKeyId={}, remaining={}/{}",
                apiKey.getId(), result.remaining(), limit);
    }

    /**
     * 检查天级限流
     *
     * @param apiKey    API Key对象
     * @param rateLimit 限流配置
     * @param response  HTTP响应
     * @throws RateLimitException 如果超过限流阈值
     */
    private void checkRateLimitPerDay(ApiKey apiKey, RateLimit rateLimit, HttpServletResponse response) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey.getId() + ":day";
        int limit = rateLimit.getPerDay();

        RateLimitResult result = rateLimitLuaScript.checkRateLimitPerDay(key, limit);

        // 如果分钟级限流没有设置响应头，则设置天级限流的响应头
        if (!rateLimit.hasMinuteLimit()) {
            setRateLimitHeaders(response, result);
        }

        if (!result.allowed()) {
            log.warn("Rate limit exceeded (per day): apiKeyId={}, limit={}", apiKey.getId(), limit);
            throw new RateLimitException(ErrorCode.API_KEY_RATE_LIMIT_EXCEEDED,
                    "Rate limit exceeded: " + limit + " requests per day");
        }

        log.debug("Rate limit check passed (per day): apiKeyId={}, remaining={}/{}",
                apiKey.getId(), result.remaining(), limit);
    }

    /**
     * 设置限流响应头
     *
     * @param response HTTP响应
     * @param result   限流结果
     */
    private void setRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader(HEADER_RATE_LIMIT, String.valueOf(result.limit()));
        response.setHeader(HEADER_RATE_REMAINING, String.valueOf(result.remaining()));
        response.setHeader(HEADER_RATE_RESET, String.valueOf(result.resetTime()));
    }
}
