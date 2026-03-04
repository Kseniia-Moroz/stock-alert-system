package com.kmoroz.stockalert.alert.controller;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.kmoroz.stockalert.common.dto.AlertDto;
import com.kmoroz.stockalert.common.dto.AlertSaveDto;

import java.util.UUID;

@Tag(name = "Alert Data", description = "Endpoints for managing alerts")
public interface AlertApi {

    @Operation(summary = "Create a new price alert", description = "Submits a new alert trigger for a specific stock symbol and target price.")
    @ApiResponse(
            responseCode = "201",
            description = "Alert created successfully",
            content = @Content(schema = @Schema(type = "string", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000"))
    )
    @ApiResponse(responseCode = "400", description = "Invalid alert parameters")
    HttpResponse<UUID> createAlert(@Valid @Body AlertSaveDto dto);

    @Operation(summary = "Get triggered alert history", description = "Retrieves a paginated list of alerts that have already been fired for a specific user.")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved history"
    )
    HttpResponse<Page<AlertDto>> getTriggeredHistory(
            @Parameter(description = "The unique ID of the user") String userId,
            @Valid Pageable pageable
    );
}
