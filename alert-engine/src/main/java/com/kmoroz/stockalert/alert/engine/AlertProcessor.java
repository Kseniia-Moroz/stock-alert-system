package com.kmoroz.stockalert.alert.engine;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import com.kmoroz.stockalert.alert.entity.Alert;
import com.kmoroz.stockalert.alert.entity.OutboxEvent;
import com.kmoroz.stockalert.alert.repository.AlertRepository;
import com.kmoroz.stockalert.alert.repository.OutboxRepository;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import com.kmoroz.stockalert.common.enums.AlertStatus;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class AlertProcessor {

    private final AlertRepository alertRepository;
    private final OutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;

    public AlertProcessor(AlertRepository alertRepository, OutboxRepository outboxRepository,
                          MeterRegistry meterRegistry) {
        this.alertRepository = alertRepository;
        this.outboxRepository = outboxRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional // Ensures the lookup and subsequent update are atomic
    public void process(PriceUpdateDto priceUpdateDto) {
        // 0. Setup a Timer to measure performance of processing of price update for stock
        Timer.Sample sample = Timer.start(meterRegistry);

        // 1. Find only PENDING alerts that cross the threshold
        log.info("Processing price update request for {} {}", priceUpdateDto.symbol(), priceUpdateDto.price());
        List<Alert> triggeredAlerts = alertRepository
                .findTriggeredAlerts(priceUpdateDto.symbol(), priceUpdateDto.price());

        if (triggeredAlerts.isEmpty()) {
            log.info("No pending alerts found");
            return;
        }

        log.info("Found {} pending alerts for {}", triggeredAlerts.size(), priceUpdateDto.symbol());

        // 2. Handle each alert
        List<OutboxEvent> eventsToSave = new ArrayList<>();
        OutboxEvent outboxEvent;
        for (Alert alert : triggeredAlerts) {
            //2.1 Mark as TRIGGERED so it doesn't fire again
            alert.setStatus(AlertStatus.TRIGGERED);

            // 2.2. Create Outbox Event
            String direction = alert.getCondition().equals("ABOVE") ? "exceeded up" : "dropped down";
            String jsonPayload = String.format("%s %s %s (Target: %s)",
                    alert.getSymbol(),
                    direction,
                    priceUpdateDto.price(),
                    alert.getTargetPrice());

            outboxEvent =  new OutboxEvent(alert.getUserId(), priceUpdateDto.eventId().toString(),
                    AlertStatus.TRIGGERED.name(), jsonPayload);
            eventsToSave.add(outboxEvent);

            incrementAlertCounter(alert);
        }
        alertRepository.saveAll(triggeredAlerts);
        outboxRepository.saveAll(eventsToSave);

        // Stop the timer and tag it with the symbol so we see which stocks take longer to process
        sample.stop(Timer.builder("stock.alerts.processing.time")
                .tag("symbol", priceUpdateDto.symbol())
                .register(meterRegistry));

          //  triggerNotification(alert, priceUpdateDto);

            // 3. Delete or mark the alert as "Fired" so the user doesn't get spammed
         //   alertRepository.delete(alert);

            // ? why do we delete alert from DB ?
            //? should we include retrieving of alerts in transaction ?
            //? should we update DB with new price target from dto ?
            //should we delete event from kafka immidiatly after it was consumed or not and why ?
            //why do we use java records for DTO objects ?
            //Outbox pattern, explaining why it is the gold standard for maintaining consistency in distributed systems
            //blue-green deployments
            // A/B testing
            //scaling (DB, cache, kafka)
            //why this system is called distributed
            //K8s Job to seed the data
            //provide a schema of the architecture

    }

    /*private void triggerNotification(Alert alert, PriceUpdateDto priceUpdateDto) {
        log.info("[NOTIFICATION] User {}! {} hit your target of {}. Current price: {}",
                alert.getUserId(), alert.getSymbol(), alert.getTargetPrice(), priceUpdateDto.price());

        // FUTURE: This is where we would call an Outbox service or a Push Notification API
    }*/

    // COUNTER WITH TAGS
    // This creates a separate time-series for every symbol/condition combination
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
