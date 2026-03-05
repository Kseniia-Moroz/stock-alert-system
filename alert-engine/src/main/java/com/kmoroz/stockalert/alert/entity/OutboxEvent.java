package com.kmoroz.stockalert.alert.entity;


import io.micronaut.data.annotation.DateCreated;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Serdeable
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateId; // e.g., Alert ID or User ID

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private String type; // e.g., "ALERT_TRIGGERED"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload; // The JSON data for the notification

    @Column(nullable = false)
    private boolean processed = false;

    private Instant processedAt;

    private String lockedBy;

    private Instant lockedAt;

    @Version
    private Long version;

    @DateCreated
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
