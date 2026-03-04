package com.kmoroz.stockalert.alert.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jpa.repository.JpaRepository;
import com.kmoroz.stockalert.alert.entity.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    // Micronaut handles the "Top 100" and "Order By" logic automatically
 //   List<OutboxEvent> findTop100ByProcessedFalseOrderByCreatedAtAsc();

    //looks for events that are either not locked at all (NULL) OR events that were locked more than 1 minute ago (< staleBefore).
   /* @Query(value = "SELECT * FROM outbox_events WHERE processed = false AND (locked_at IS NULL OR locked_at < :staleBefore) ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<OutboxEvent> findCandidates(Instant staleBefore, int limit);

    @Query(value = "UPDATE outbox_events SET locked_by = :instanceId, locked_at = :now, version = version + 1 WHERE id = :id AND version = :expectedVersion", nativeQuery = true)
    int tryLockEvent(UUID id, Long expectedVersion, String instanceId, Instant now);

    void deleteByProcessedTrueAndCreatedAtBefore(Instant threshold);*/

    /**
     * ATOMIC CLAIM PATTERN:
     * 1. Finds the next available (unprocessed and unlocked) rows.
     * 2. Skips any rows already locked by other pods (SKIP LOCKED).
     * 3. Immediately marks them as locked by this pod instance.
     * 4. Returns the full entities in one single DB round-trip.
     */
  /*  @Query(value = """
        UPDATE outbox_events 
        SET locked_by = :instanceId, 
            locked_at = :now, 
            version = version + 1 
        WHERE id IN (
            SELECT id FROM outbox_events 
            WHERE processed = false 
              AND (locked_at IS NULL OR locked_at < :staleBefore)
            ORDER BY created_at ASC 
            LIMIT :limit 
            FOR UPDATE SKIP LOCKED
        ) 
        RETURNING *
        """, nativeQuery = true)
    List<OutboxEvent> lockAndFetch(String instanceId, Instant now, Instant staleBefore, int limit);*/

    /**
     * ATOMIC CLAIM PATTERN:
     * 1. Finds the next available (unprocessed and unlocked) rows.
     * 2. Skips any rows already locked by other pods (SKIP LOCKED).
     * 3. Immediately marks them as locked by this pod instance.
     * 4. Returns the full entities in one single DB round-trip.
     */
    @Query(value = """
    WITH updated_rows AS (
        UPDATE outbox_events 
        SET locked_by = :instanceId, 
            locked_at = :now, 
            version = version + 1 
        WHERE id IN (
            SELECT id FROM outbox_events 
            WHERE processed = false 
              AND (locked_at IS NULL OR locked_at < :staleBefore)
            ORDER BY created_at ASC 
            LIMIT :limit 
            FOR UPDATE SKIP LOCKED
        ) 
        RETURNING *
    )
    SELECT * FROM updated_rows
    """, nativeQuery = true)
    List<OutboxEvent> lockAndFetch(String instanceId, Instant now, Instant staleBefore, int limit);
}
