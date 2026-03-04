package com.kmoroz.stockalert.alert.entity;

import io.micronaut.data.annotation.DateCreated;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Serdeable
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private String consumerGroup;

    @DateCreated
    @Column(nullable = false, updatable = false)
    private Instant processedAt;
    
    public ProcessedEvent(UUID eventId, String consumerGroup) {
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
    }
}
