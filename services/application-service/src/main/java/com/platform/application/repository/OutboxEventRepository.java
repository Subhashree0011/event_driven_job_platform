package com.platform.application.repository;

import com.platform.application.outbox.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Find unpublished events that haven't exceeded max retry count.
     * Ordered by created_at to process oldest first (FIFO).
     * Uses LIMIT via native query for batch processing.
     */
    @Query(value = "SELECT * FROM outbox_events " +
                   "WHERE published = false AND retry_count < :maxRetries " +
                   "ORDER BY created_at ASC LIMIT :limit",
            nativeQuery = true)
    List<OutboxEvent> findUnpublishedEvents(
            @Param("maxRetries") int maxRetries,
            @Param("limit") int limit
    );

    /**
     * Count unpublished events — for health check / monitoring.
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.published = false")
    long countUnpublished();

    /**
     * Count events that have exceeded retry limit (dead letters).
     */
    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.published = false AND o.retryCount >= :maxRetries")
    long countDeadLetters(@Param("maxRetries") int maxRetries);
}
