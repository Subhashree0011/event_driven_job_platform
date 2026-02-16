package com.platform.application.config;

import org.springframework.context.annotation.Configuration;

/**
 * Circuit breaker configuration — declarative via application.yml.
 *
 * Configured instances:
 * - databaseCircuitBreaker: 50% failure rate, 30s open state
 * - kafkaCircuitBreaker: 50% failure rate, 20s open state
 */
@Configuration
public class CircuitBreakerConfig {
    // Configuration is declarative via application.yml
}
