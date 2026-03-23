package com.fsa.aicenter.common.config;

import com.fsa.aicenter.interfaces.filter.ApiKeyAuthenticationFilter;
import com.fsa.aicenter.interfaces.filter.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Filter配置类
 * 注册认证和限流过滤器，并设置执行顺序
 *
 * @author FSA AI Center
 */
@Configuration
public class FilterConfig {

    /**
     * 注册API Key认证过滤器
     * Order = 1，最先执行
     */
    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilterRegistration(
            ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*", "/v1/*");
        registration.setName("apiKeyAuthFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    /**
     * 注册限流过滤器
     * Order = 2，在认证过滤器之后执行
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*", "/v1/*");
        registration.setName("rateLimitFltr");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }
}
