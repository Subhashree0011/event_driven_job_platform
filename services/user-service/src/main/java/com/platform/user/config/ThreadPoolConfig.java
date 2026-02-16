package com.platform.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Bounded thread pool configuration.
 * 
 * Design Decision: Bounded pools create backpressure instead of
 * memory exhaustion. Pool sizes aligned with downstream capacity.
 * 
 * Formula: async pool < HikariCP pool < Tomcat threads
 * This ensures we never overwhelm the database.
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);          // Bounded queue — rejects when full
        executor.setThreadNamePrefix("user-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
