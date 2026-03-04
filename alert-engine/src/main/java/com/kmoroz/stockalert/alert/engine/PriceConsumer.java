package com.kmoroz.stockalert.alert.engine;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import com.kmoroz.stockalert.alert.repository.ProcessedEventRepository;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import org.slf4j.MDC;

import static com.kmoroz.stockalert.common.AlertSystemConstants.CORRELATION_ID_KEY;

@KafkaListener(groupId = "alert-engine", offsetReset = OffsetReset.EARLIEST)
@Singleton
@Slf4j
public class PriceConsumer {

    private final AlertProcessor alertProcessor;
    private final ProcessedEventRepository processedEventRepository;

    public PriceConsumer(AlertProcessor alertProcessor, ProcessedEventRepository processedEventRepository) {
        this.alertProcessor = alertProcessor;
        this.processedEventRepository = processedEventRepository;
    }


    @ExecuteOn(TaskExecutors.IO) // Runs on a Virtual Thread in Java 21
    @Topic("market-prices")
    @Transactional // Atomically covers idempotency check and alert processing
    @Retryable(attempts = "5", delay = "2s", multiplier = "2.0")
    public void receive(PriceUpdateDto priceUpdate) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(CORRELATION_ID_KEY, priceUpdate.eventId().toString())) {
            log.info("Received price for {}: ${}", priceUpdate.symbol(), priceUpdate.price());
            //we need idempotency check to handle Kafka's "at-least-once" delivery guarantee
            // If the insert succeeds, the event is new, and we process the alerts.
            // If it fails (duplicate key), we know we've already handled this specific price tick, and we safely ignore it
            int inserted = processedEventRepository.tryInsert(priceUpdate.eventId(), "alert-engine");
            if (inserted == 0) {
                log.info("Duplicate event {} ignored", priceUpdate.eventId());
                return;
            }

            // This is where we check if any user has an alert for this price
            alertProcessor.process(priceUpdate);
        }
    }
}
