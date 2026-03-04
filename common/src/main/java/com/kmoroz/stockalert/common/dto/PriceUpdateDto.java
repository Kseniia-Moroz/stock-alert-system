package com.kmoroz.stockalert.common.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Serdeable    // Allows Micronaut Serialization (Jackson replacement) to handle this
@Introspected // Tells Micronaut to generate bean metadata at compile-time
public record PriceUpdateDto(
        UUID eventId,       // Unique ID for idempotency, also acts as correlation ID
        String symbol,      // e.g., "TSLA", "AAPL"
        BigDecimal price,   // ALWAYS use BigDecimal for financial data
        Instant timestamp
) {
}
