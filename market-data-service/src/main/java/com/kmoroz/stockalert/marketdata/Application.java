package com.kmoroz.stockalert.marketdata;
/**
 * Market Data Service module Application.
 */

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Market Data Service API",
                version = "1.0",
                description = "API for querying real-time stock prices."
        )
)
public class Application {
    public static void main(String[] args) {
        System.out.printf("Market data service module");
        Micronaut.run(Application.class, args);
    }
}