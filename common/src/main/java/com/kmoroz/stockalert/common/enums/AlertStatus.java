package com.kmoroz.stockalert.common.enums;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum AlertStatus {
    PENDING, TRIGGERED, CANCELLED
}
