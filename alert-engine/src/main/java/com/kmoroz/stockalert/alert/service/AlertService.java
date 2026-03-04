package com.kmoroz.stockalert.alert.service;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import com.kmoroz.stockalert.alert.entity.Alert;
import com.kmoroz.stockalert.alert.repository.AlertRepository;
import com.kmoroz.stockalert.common.dto.AlertDto;
import com.kmoroz.stockalert.common.dto.AlertSaveDto;

import java.util.UUID;

import static com.kmoroz.stockalert.common.enums.AlertStatus.TRIGGERED;

@Singleton
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public Page<AlertDto> getAlertHistory(String userId, Pageable pageable) {
        log.info("Getting alert history");
        Page<Alert> alerts = alertRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, TRIGGERED, pageable);
        return alerts.map(alert -> AlertDto.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .symbol(alert.getSymbol())
                .targetPrice(alert.getTargetPrice())
                .condition(alert.getCondition())
                .status(alert.getStatus())
                .createdAt(alert.getCreatedAt())
                .build()
        );
    }

    public UUID createAlert(AlertSaveDto dto) {
        log.info("Create new alert for {} with target price {} and condition {}", dto.symbol(), dto.targetPrice(), dto.condition());
        Alert savedAlert = alertRepository.save(initializeAlert(dto));
        return savedAlert.getId();
    }

    private Alert initializeAlert(AlertSaveDto dto) {
        Alert alert = new Alert();
        alert.setUserId(dto.userId());
        alert.setSymbol(dto.symbol());
        alert.setTargetPrice(dto.targetPrice());
        alert.setCondition(dto.condition());
        return alert;
    }
}
