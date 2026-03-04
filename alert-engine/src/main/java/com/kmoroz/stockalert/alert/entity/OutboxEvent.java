package com.kmoroz.stockalert.alert.entity;


import io.micronaut.data.annotation.DateCreated;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import java.time.Instant;
import java.util.UUID;

@Serdeable
@Entity
@DynamicUpdate
@OptimisticLocking(type = OptimisticLockType.DIRTY) // Track changes to any field
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
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

    public OutboxEvent(String aggregateId, String correlationId, String type, String payload) {
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
        this.type = type;
        this.payload = payload;
    }
}
