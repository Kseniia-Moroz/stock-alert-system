package com.kmoroz.stockalert.common.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record InstrumentDto(String symbol, String name, String sector) {
}
