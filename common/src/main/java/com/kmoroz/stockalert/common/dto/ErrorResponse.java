package com.kmoroz.stockalert.common.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;import lombok.Builder;import java.time.Instant;

@Serdeable
@Introspected
@Builder
public record ErrorResponse(Integer code, String reason, String message, Instant timestamp) {
}
