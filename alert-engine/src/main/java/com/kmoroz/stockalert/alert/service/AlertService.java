package com.kmoroz.stockalert.alert.service;

import com.kmoroz.stockalert.alert.entity.Alert;
import com.kmoroz.stockalert.alert.repository.AlertRepository;
import com.kmoroz.stockalert.common.dto.AlertDto;
import com.kmoroz.stockalert.common.dto.AlertSaveDto;
import com.kmoroz.stockalert.common.enums.AlertStatus;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.kmoroz.stockalert.common.enums.AlertStatus.TRIGGERED;

/**
 * Service responsible for managing user alerts and alert history.
 *
 * This class is a Micronaut {@link Singleton} that provides operations to retrieve
 * a user's triggered alert history and create new stock price alerts. It interacts
 * with the {@link AlertRepository} for persistence.
 *
 * @author kmoroz
 */
@Singleton
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Retrieves the history of triggered alerts for a specific user.
     *
     * @param userId the unique identifier of the user
     * @param pageable pagination and sorting information
     * @return a {@link Page} of {@link AlertDto} objects representing the triggered alerts
     */
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

    /**
     * Creates and saves a new stock alert.
     *
     * @param dto the data required to create a new alert
     * @return the {@link UUID} of the newly created alert
     */
    public UUID createAlert(AlertSaveDto dto) {
        log.info("Create new alert for {} with target price {} and condition {}", dto.symbol(), dto.targetPrice(), dto.condition());
        Alert savedAlert = alertRepository.save(initializeAlert(dto));
        return savedAlert.getId();
    }

    private Alert initializeAlert(AlertSaveDto dto) {
        return Alert.builder()
                .userId(dto.userId())
                .symbol(dto.symbol())
                .status(AlertStatus.PENDING)
                .targetPrice(dto.targetPrice())
                .condition(dto.condition())
                .build();
    }
}
