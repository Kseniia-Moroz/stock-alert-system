package com.kmoroz.stockalert.common.dto;

import com.kmoroz.stockalert.common.enums.AlertCondition;
import com.kmoroz.stockalert.common.enums.AlertStatus;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Serdeable
@Builder
public record AlertDto(
        @NotNull UUID id,
        @NotBlank String userId,
        @NotBlank String symbol,
        @NotNull @DecimalMin("0.01") BigDecimal targetPrice,
        @NotNull AlertCondition condition,
        @NotNull AlertStatus status,
        @NotNull Instant createdAt) {
}
