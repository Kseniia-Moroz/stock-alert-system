package com.kmoroz.stockalert.alert.entity;

import io.micronaut.data.annotation.DateCreated;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.kmoroz.stockalert.common.enums.AlertCondition;
import com.kmoroz.stockalert.common.enums.AlertStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Serdeable
@Table(name = "alerts")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal targetPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertCondition condition; // ABOVE or BELOW

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.PENDING; // PENDING, TRIGGERED, CANCELLED

    @DateCreated
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
