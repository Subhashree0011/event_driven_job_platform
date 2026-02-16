package com.platform.application.config;

import org.springframework.context.annotation.Configuration;

/**
 * Database configuration.
 * JPA/Hibernate settings are in application.yml.
 * Uses READ COMMITTED isolation for write-heavy application workload.
 * Batch inserts enabled (batch_size=50, order_inserts=true).
 *
 * Interview Talking Point:
 * - READ COMMITTED prevents dirty reads but allows non-repeatable reads
 * - Good trade-off for write-heavy workloads: less locking contention
 * - Batch inserts with order_inserts reduces round trips for bulk operations
 */
@Configuration
public class DatabaseConfig {
    // Declarative configuration via application.yml
}
