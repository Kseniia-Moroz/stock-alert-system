package com.kmoroz.stockalert.common.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Serdeable
@Builder
public record PriceUpdateDto(
        UUID eventId,       // Unique ID for idempotency, also acts as correlation ID
        String symbol,
        BigDecimal price,
        Instant timestamp
) {
}
