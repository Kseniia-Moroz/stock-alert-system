package com.kmoroz.stockalert.common.enums;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum AlertCondition {
    ABOVE, BELOW
}
