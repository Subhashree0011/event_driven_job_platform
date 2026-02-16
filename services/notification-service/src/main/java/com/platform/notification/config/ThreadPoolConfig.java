package com.platform.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Thread pool configuration for Notification Service.
 * 
 * Bulkhead pattern: Separate pools for different concerns
 *   - Email sending: I/O-heavy (SMTP), needs its own pool
 *   - SMS sending: External API calls, isolated from email
 *   - General async: Logging, metrics, cleanup
 * 
 * Why bounded:
 *   - Unbounded threads → memory explosion → GC storms (Incident #1)
 *   - CallerRunsPolicy creates backpressure instead of OOM
 *   - Each pool sized to downstream capacity (SMTP connection limits, API rate limits)
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /**
     * Email sending executor.
     * Core=5, Max=10 — limited by SMTP connection pool.
     * Queue=50 — buffer for spikes, then backpressure.
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * SMS sending executor.
     * Smaller pool — SMS APIs typically have stricter rate limits.
     */
    @Bean(name = "smsExecutor")
    public Executor smsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(30);
        executor.setThreadNamePrefix("sms-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * General async executor for non-critical tasks.
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
