package com.platform.job.config;

import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j circuit breaker configuration.
 * Actual config is in application.yml under resilience4j.circuitbreaker.instances.
 * This class exists as a placeholder for programmatic customization if needed.
 *
 * Configured instances:
 * - databaseCircuitBreaker: 50% failure rate, 30s open state
 * - redisCircuitBreaker: 50% failure rate, 15s open state
 */
@Configuration
public class CircuitBreakerConfig {
    // Configuration is declarative via application.yml
    // Add @Bean customizations here if programmatic config is needed
}
