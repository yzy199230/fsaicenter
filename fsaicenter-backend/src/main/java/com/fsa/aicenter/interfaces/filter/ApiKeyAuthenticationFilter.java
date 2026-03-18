package com.fsa.aicenter.interfaces.filter;

import com.fsa.aicenter.common.exception.AuthenticationException;
import com.fsa.aicenter.common.exception.ErrorCode;
import com.fsa.aicenter.domain.apikey.aggregate.ApiKey;
import com.fsa.aicenter.domain.apikey.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * API Key认证过滤器
 * 从请求头中提取API Key，验证其有效性，并将其放入请求属性中供后续使用
 *
 * @author FSA AI Center
 */
@Slf4j
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Authorization请求头名称
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Bearer前缀
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * API Key请求属性名
     */
    public static final String API_KEY_ATTRIBUTE = "apiKey";

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 提取API Key
            String apiKeyValue = extractApiKey(request);

            // 2. 验证API Key
            ApiKey apiKey = validateApiKey(apiKeyValue, request);

            // 3. 放入请求属性
            request.setAttribute(API_KEY_ATTRIBUTE, apiKey);

            // 4. 继续过滤链
            filterChain.doFilter(request, response);

        } catch (AuthenticationException e) {
            // 认证失败，记录日志
            log.warn("API Key authentication failed: {} - IP: {}, Path: {}",
                    e.getMessage(),
                    getClientIp(request),
                    request.getRequestURI());
            throw e;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 排除不需要认证的路径
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
     * 从请求头中提取API Key
     *
     * @param request HTTP请求
     * @return API Key值
     * @throws AuthenticationException 如果API Key缺失或格式错误
     */
    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authHeader)) {
            throw new AuthenticationException("API Key is required");
        }

        if (!authHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationException("Invalid Authorization header format. Expected: Bearer <api_key>");
        }

        String apiKey = authHeader.substring(BEARER_PREFIX.length()).trim();

        if (!StringUtils.hasText(apiKey)) {
            throw new AuthenticationException("API Key is required");
        }

        return apiKey;
    }

    /**
     * 验证API Key的有效性
     *
     * @param apiKeyValue API Key值
     * @param request     HTTP请求
     * @return API Key对象
     * @throws AuthenticationException 如果API Key无效
     */
    private ApiKey validateApiKey(String apiKeyValue, HttpServletRequest request) {
        // 查询API Key
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyValue(apiKeyValue);

        if (apiKeyOpt.isEmpty()) {
            throw new AuthenticationException(ErrorCode.API_KEY_INVALID, "Invalid API Key");
        }

        ApiKey apiKey = apiKeyOpt.get();

        // 检查是否启用
        if (!apiKey.isEnabled()) {
            throw new AuthenticationException(ErrorCode.FORBIDDEN, "API Key is disabled");
        }

        // 检查是否过期
        if (apiKey.isExpired()) {
            throw new AuthenticationException(ErrorCode.API_KEY_EXPIRED, "API Key has expired");
        }

        // 检查IP白名单（如果配置了）
        String clientIp = getClientIp(request);
        if (!apiKey.isIpAllowed(clientIp)) {
            log.warn("API Key IP not allowed: keyId={}, clientIp={}", apiKey.getId(), clientIp);
            throw new AuthenticationException(ErrorCode.API_KEY_IP_NOT_ALLOWED,
                    "IP address not allowed: " + clientIp);
        }

        log.debug("API Key authenticated successfully: keyId={}, keyName={}, ip={}",
                apiKey.getId(), apiKey.getKeyName(), clientIp);

        return apiKey;
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求
     * @return 客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        // 尝试从X-Forwarded-For头获取（适用于通过代理的请求）
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index).trim();
            }
            return ip.trim();
        }

        // 尝试从X-Real-IP头获取
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        // 直接从请求中获取
        return request.getRemoteAddr();
    }
}
