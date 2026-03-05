package com.kmoroz.stockalert.common.dto;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;

import java.time.Instant;

@Serdeable
@Builder
public record ErrorResponse(Integer code, String reason, String message, Instant timestamp) {
}
