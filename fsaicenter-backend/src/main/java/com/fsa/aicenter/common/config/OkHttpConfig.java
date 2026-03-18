package com.fsa.aicenter.common.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp配置
 *
 * @author FSA AI Center
 */
@Configuration
public class OkHttpConfig {

    /**
     * 创建通用OkHttpClient（用于普通HTTP请求）
     */
    @Bean("commonOkHttpClient")
    public OkHttpClient commonOkHttpClient() {
        return new OkHttpClient.Builder()
                // 连接超时
                .connectTimeout(Duration.ofSeconds(10))
                // 读取超时
                .readTimeout(Duration.ofSeconds(30))
                // 写入超时
                .writeTimeout(Duration.ofSeconds(30))
                // 连接池配置（最大空闲连接数5，连接存活时间5分钟）
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                // 失败重试
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * 创建AI调用专用OkHttpClient（超时时间更长）
     */
    @Bean("aiOkHttpClient")
    public OkHttpClient aiOkHttpClient() {
        return new OkHttpClient.Builder()
                // AI调用连接超时
                .connectTimeout(Duration.ofSeconds(10))
                // AI调用读取超时（支持流式响应和长时间处理）
                .readTimeout(Duration.ofSeconds(120))
                // AI调用写入超时
                .writeTimeout(Duration.ofSeconds(60))
                // 连接池配置（AI调用并发量大，增加连接数）
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
                // 失败重试
                .retryOnConnectionFailure(true)
                .build();
    }
}
