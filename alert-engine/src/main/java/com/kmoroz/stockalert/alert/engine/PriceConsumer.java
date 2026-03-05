package com.kmoroz.stockalert.alert.engine;

import com.kmoroz.stockalert.alert.repository.ProcessedEventRepository;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import static com.kmoroz.stockalert.common.AlertSystemConstants.CORRELATION_ID_KEY;

/**
 * Consumes real-time stock price updates from a Kafka topic and triggers alert processing.
 *
 * This class is a Micronaut {@link KafkaListener} and a {@link Singleton}. It processes
 * {@link PriceUpdateDto} messages received from the {@code market-prices} topic.
 * To ensure reliability and performance, it uses Java 21 Virtual Threads (via {@link ExecuteOn})
 * and implements idempotency to handle Kafka's at-least-once delivery guarantee.
 *
 * @author kmoroz
 */
@KafkaListener(groupId = "alert-engine", offsetReset = OffsetReset.EARLIEST)
@Singleton
@Slf4j
public class PriceConsumer {

    private final PriceAlertService priceAlertService;
    private final ProcessedEventRepository processedEventRepository;

    public PriceConsumer(PriceAlertService priceAlertService, ProcessedEventRepository processedEventRepository) {
        this.priceAlertService = priceAlertService;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Receives and processes a single stock price update.
     *
     * This method:
     * 1. Sets the correlation ID in the {@link MDC} for logging.
     * 2. Performs an idempotency check by attempting to insert the event ID into the database.
     * 3. If the event is new, it delegates alert processing to {@link PriceAlertService}.
     *
     * The method is {@link Transactional}, ensuring that the idempotency check and subsequent
     * alert processing are performed atomically. It is also {@link Retryable} to handle
     * transient failures.
     *
     * @param priceUpdate the price update data received from Kafka
     */
    @ExecuteOn(TaskExecutors.IO)
    @Topic("market-prices")
    @Transactional
    @Retryable(attempts = "5", delay = "2s", multiplier = "2.0")
    public void receive(PriceUpdateDto priceUpdate) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(CORRELATION_ID_KEY, priceUpdate.eventId().toString())) {
            log.info("Received price for {}: ${}", priceUpdate.symbol(), priceUpdate.price());
            //we need idempotency check to handle Kafka's "at-least-once" delivery guarantee
            // If the insert succeeds, the event is new, and we process the alerts
            // If it fails (duplicate key), we know we've already handled this specific price tick, and we safely ignore it
            int inserted = processedEventRepository.tryInsert(priceUpdate.eventId(), "alert-engine");
            if (inserted == 0) {
                log.info("Duplicate event {} ignored", priceUpdate.eventId());
                return;
            }

            // heck if any user has an alert for this price
            priceAlertService.process(priceUpdate);
        }
    }
}
