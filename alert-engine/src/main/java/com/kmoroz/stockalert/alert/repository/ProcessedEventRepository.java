package com.kmoroz.stockalert.alert.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import com.kmoroz.stockalert.alert.entity.ProcessedEvent;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    @Query(value = "INSERT INTO processed_events(event_id, consumer_group, processed_at) VALUES (:eventId, :consumerGroup, NOW()) ON CONFLICT DO NOTHING", nativeQuery = true)
    int tryInsert(UUID eventId, String consumerGroup);
}
