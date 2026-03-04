package com.kmoroz.stockalert.marketdata.controller;

import io.micronaut.http.HttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "Market Data", description = "Endpoints for retrieving real-time stock prices")
public interface PriceApi {

    @Operation(summary = "Get latest stock price")
    @ApiResponse(
            responseCode = "200",
            description = "Price retrieved successfully",
            content = @Content(schema = @Schema(example = "{\"price\": \"150.25\"}"))
    )
    @ApiResponse(responseCode = "404", description = "Price not found")
    HttpResponse<Map<String, String>> getLatestPrice(String symbol);
}
