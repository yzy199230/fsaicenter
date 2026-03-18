package com.fsa.aicenter.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 定时任务配置
 * <p>
 * 启用Spring定时任务功能，配置任务调度器。
 * JDK 21+使用Virtual Thread，低版本使用线程池。
 * </p>
 *
 * @author FSA AI Center
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
    }

    /**
     * 配置任务调度器
     * <p>
     * 使用线程池任务调度器，支持并发执行多个定时任务
     * </p>
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setErrorHandler(throwable ->
            log.error("定时任务执行异常", throwable)
        );
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();

        log.info("初始化定时任务调度器: ThreadPoolTaskScheduler (poolSize=5)");
        return scheduler;
    }
}
