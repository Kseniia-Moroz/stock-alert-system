package com.kmoroz.stockalert.alert.controller;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;
import com.kmoroz.stockalert.alert.service.AlertService;
import com.kmoroz.stockalert.common.dto.AlertDto;
import com.kmoroz.stockalert.common.dto.AlertSaveDto;

import java.util.UUID;

@Controller("/alerts")
public class AlertController implements AlertApi {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @Post
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<UUID> createAlert(@Valid @Body AlertSaveDto dto) {
        return HttpResponse.created(alertService.createAlert(dto));
    }

    @Get("/history/{userId}")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<Page<AlertDto>> getTriggeredHistory(String userId, Pageable pageable) {
        // This will return all alerts that have already fired
        return HttpResponse.ok(alertService.getAlertHistory(userId, pageable));
    }
}
