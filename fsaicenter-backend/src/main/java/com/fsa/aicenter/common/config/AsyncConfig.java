package com.fsa.aicenter.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 异步任务配置 - 优先使用JDK 21 Virtual Thread，低版本自动降级
 *
 * @author FSA AI Center
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 配置异步执行器
     * JDK 21+使用Virtual Thread，低版本使用线程池
     */
    @Override
    public Executor getAsyncExecutor() {
        return createExecutor("async-executor");
    }

    /**
     * 操作日志异步执行器
     * 提供给@Async("asyncExecutor")使用
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        return createExecutor("log-executor");
    }

    /**
     * 创建执行器 - 自动检测JDK版本选择最佳实现
     * 使用反射避免编译时依赖JDK 21
     */
    private Executor createExecutor(String name) {
        // 尝试通过反射使用Virtual Thread (JDK 21+)
        try {
            java.lang.reflect.Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            Executor virtualExecutor = (Executor) method.invoke(null);
            log.info("初始化异步执行器 [{}]: Virtual Thread Executor (JDK 21+)", name);
            return virtualExecutor;
        } catch (NoSuchMethodException e) {
            // JDK版本低于21，降级到传统线程池
            log.info("当前JDK不支持Virtual Thread，使用传统线程池 [{}]", name);
            return createFallbackExecutor(name);
        } catch (Exception e) {
            // 其他异常，降级到传统线程池
            log.warn("Virtual Thread初始化失败，降级到传统线程池 [{}]: {}", name, e.getMessage());
            return createFallbackExecutor(name);
        }
    }

    /**
     * 创建后备线程池执行器
     */
    private Executor createFallbackExecutor(String name) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix(name + "-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("初始化异步执行器 [{}]: ThreadPoolTaskExecutor (core=10, max=50)", name);
        return executor;
    }

    /**
     * 异步任务异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("异步任务执行异常 - 方法: {}.{}, 参数: {}",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        params,
                        ex);
            }
        };
    }
}
