package com.fsa.aicenter.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * sa-token拦截器配置
 * <p>
 * 配置全局路由拦截规则,对管理后台API进行登录验证
 * </p>
 *
 * @author FSA AI Center
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 禁用async超时，由上游API连接本身控制生命周期
        configurer.setDefaultTimeout(-1);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册sa-token拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 指定所有/admin路由都需要登录认证
            SaRouter.match("/admin/**")
                // 排除登录接口
                .notMatch("/admin/auth/login")
                // 排除Knife4j文档
                .notMatch("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**")
                // 执行登录检查
                .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
