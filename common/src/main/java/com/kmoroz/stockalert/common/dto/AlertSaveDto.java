package com.kmoroz.stockalert.common.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.kmoroz.stockalert.common.enums.AlertCondition;

import java.math.BigDecimal;

@Serdeable
@Introspected
public record AlertSaveDto(
        @NotBlank String userId,
        @NotBlank String symbol,
        @NotNull @DecimalMin("0.01") BigDecimal targetPrice,
        @NotNull AlertCondition condition) {
}