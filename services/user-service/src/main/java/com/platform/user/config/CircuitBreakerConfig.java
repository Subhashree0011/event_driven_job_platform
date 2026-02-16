package com.platform.user.config;

import org.springframework.context.annotation.Configuration;

/**
 * Circuit breaker configuration.
 * Configuration is externalized in application.yml under resilience4j section.
 * 
 * This class exists for any programmatic circuit breaker customization.
 * Current setup uses declarative config via @CircuitBreaker annotations
 * and application.yml properties.
 * 
 * Circuit Breakers configured:
 * - databaseCircuitBreaker: Opens after 50% failures in sliding window of 10
 * - redisCircuitBreaker: Opens after 50% failures, shorter wait in open state
 */
@Configuration
public class CircuitBreakerConfig {
    // Configuration is declarative via application.yml
    // See resilience4j.circuitbreaker section
}
