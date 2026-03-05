package com.kmoroz.stockalert.common.dto;

import com.kmoroz.stockalert.common.enums.AlertCondition;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Serdeable
@Builder
public record AlertSaveDto(
        @NotBlank String userId,
        @NotBlank String symbol,
        @NotNull @DecimalMin("0.01") BigDecimal targetPrice,
        @NotNull AlertCondition condition) {
}