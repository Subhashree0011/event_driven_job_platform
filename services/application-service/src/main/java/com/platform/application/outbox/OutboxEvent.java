package com.platform.application.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Outbox event entity — written in the same transaction as the domain event.
 * A background poller reads unpublished events and publishes them to Kafka.
 *
 * Interview Talking Point:
 * - Transactional Outbox Pattern guarantees at-least-once delivery
 * - Same DB transaction for domain write + outbox write = atomicity
 * - If Kafka is down, events accumulate in outbox table and are retried
 * - retry_count prevents infinite retry loops (max 5 attempts)
 * - Partition key ensures ordering per aggregate (job_id)
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_unpublished", columnList = "published, created_at"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_outbox_retry", columnList = "published, retry_count")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key")
    private String partitionKey;

    @Column(nullable = false)
    @Builder.Default
    private Boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // === Business Methods ===

    public boolean isRetryable(int maxRetries) {
        return !published && retryCount < maxRetries;
    }

    public void markPublished() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
