package com.kmoroz.stockalert.alert.engine;

import com.kmoroz.stockalert.alert.entity.Alert;
import com.kmoroz.stockalert.alert.entity.OutboxEvent;
import com.kmoroz.stockalert.alert.repository.AlertRepository;
import com.kmoroz.stockalert.alert.repository.OutboxRepository;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import com.kmoroz.stockalert.common.enums.AlertStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Processes real-time stock price updates to trigger user-defined alerts.
 *
 * This class is a Micronaut {@link Singleton} responsible for evaluating price ticks against
 * pending {@link Alert} entities. When a price threshold is crossed, it transitions the alert
 * to a triggered state and records an {@link OutboxEvent} to satisfy the Transactional Outbox pattern.
 * It also records performance and business metrics using Micrometer.
 *
 * @author kmoroz
 */
@Singleton
@Slf4j
public class PriceAlertService {

    private final AlertRepository alertRepository;
    private final OutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;

    public PriceAlertService(AlertRepository alertRepository, OutboxRepository outboxRepository,
                             MeterRegistry meterRegistry) {
        this.alertRepository = alertRepository;
        this.outboxRepository = outboxRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Processes a single price update for a stock symbol.
     *
     * This method:
     * 1. Starts a {@link Timer} to measure the execution time of the processing logic.
     * 2. Queries the database for pending alerts whose price thresholds have been crossed.
     * 3. For each triggered alert, updates its status and creates a corresponding outbox event.
     * 4. Persists all changes atomically within a database transaction.
     *
     * @param priceUpdateDto the price update information received from the market data source
     */
    @Transactional
    public void process(PriceUpdateDto priceUpdateDto) {
        // Setup a Timer to measure performance of processing of price update for stock
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Find only PENDING alerts that cross the threshold
            log.info("Processing price update request for {} {}", priceUpdateDto.symbol(), priceUpdateDto.price());
            List<Alert> pendingAlerts = alertRepository
                    .findPendingAlerts(priceUpdateDto.symbol(), priceUpdateDto.price());

            if (pendingAlerts.isEmpty()) {
                log.info("No pending alerts found");
                return;
            }

            log.info("Found {} pending alerts for {}", pendingAlerts.size(), priceUpdateDto.symbol());

            // Handle each alert
            List<OutboxEvent> eventsToSave = pendingAlerts.stream()
                    .map(alert -> handleAlertTrigger(alert, priceUpdateDto))
                    .toList();

            alertRepository.saveAll(pendingAlerts);
            outboxRepository.saveAll(eventsToSave);
        } finally {
            // Stop the timer and tag it with the symbol to see which stocks take longer to process
            sample.stop(Timer.builder("stock.alerts.processing.time")
                    .tag("symbol", priceUpdateDto.symbol())
                    .register(meterRegistry));
        }
    }

    /**
     * Updates an alert's status to {@code TRIGGERED} and prepares a corresponding outbox event.
     *
     * @param alert the alert to be triggered
     * @param priceUpdateDto the price update that triggered the alert
     * @return the resulting {@link OutboxEvent} to be persisted
     */
    private OutboxEvent handleAlertTrigger(Alert alert, PriceUpdateDto priceUpdateDto) {
        alert.setStatus(AlertStatus.TRIGGERED);
        incrementAlertCounter(alert);
        return createEvent(alert, priceUpdateDto);
    }

    /**
     * Creates an {@link OutboxEvent} from a triggered alert.
     *
     * @param alert the triggered alert
     * @param priceUpdateDto the price update that triggered the alert
     * @return a new {@link OutboxEvent} populated with notification details
     */
    private OutboxEvent createEvent(Alert alert, PriceUpdateDto priceUpdateDto) {
        String direction = alert.getCondition().equals("ABOVE") ? "exceeded up" : "dropped down";
        String jsonPayload = alert.getSymbol() + " " + direction + " " + priceUpdateDto.price() +
                " (Target: " + alert.getTargetPrice() + ")";

        return OutboxEvent.builder()
                .aggregateId(alert.getUserId())
                .correlationId(priceUpdateDto.eventId().toString())
                .type(AlertStatus.TRIGGERED.name())
                .payload(jsonPayload)
                .build();
    }

    /**
     * Increments a business metric counter when an alert is triggered.
     *
     * @param alert the alert that was triggered
     */
    private void incrementAlertCounter(Alert alert) {
        Counter.builder("stock.alerts.triggered")
                .description("Total number of triggered alerts")
                .tag("symbol", alert.getSymbol())
                .tag("condition", String.valueOf(alert.getCondition()))
                .tag("service", "alert-engine")
                .register(meterRegistry)
                .increment();
    }
}
