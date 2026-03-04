package com.kmoroz.stockalert.alert;
/**
 * Alert Engine module Application.
 */

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Alert Engine API",
                version = "1.0",
                description = "API for managing stock price alerts and notification outbox."
        )
)
public class Application {
    public static void main(String[] args) {
        System.out.printf("Alert engine module");
        Micronaut.run(Application.class, args);
    }
}