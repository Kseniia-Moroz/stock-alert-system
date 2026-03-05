package com.kmoroz.stockalert.alert.engine;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.json.JsonMapper;
import io.micronaut.retry.annotation.Retryable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import lombok.extern.slf4j.Slf4j;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import org.slf4j.MDC;

import static com.kmoroz.stockalert.common.AlertSystemConstants.CORRELATION_ID_KEY;
import static com.kmoroz.stockalert.common.AlertSystemConstants.STOCK_PRICE;

/**
 * Consumes real-time stock price updates from Kafka and materializes them into a Redis data store.
 *
 * This class is a Micronaut {@link KafkaListener} and is responsible for maintaining the latest
 * price for each stock symbol in Redis. This allows other services to quickly retrieve current
 * prices without querying Kafka or the database directly. It uses Java 21 Virtual Threads
 * for efficient I/O processing.
 *
 * @author kmoroz
 */
@KafkaListener(groupId = "price-materializer")
@Slf4j
public class PriceMaterializer {

    private final StatefulRedisConnection<String, String> connection;

    public PriceMaterializer(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    /**
     * Receives a single stock price update and materializes it into Redis.
     *
     * This method:
     * 1. Sets the correlation ID in the {@link MDC} for logging.
     * 2. Formats a Redis key using the stock symbol.
     * 3. Sets the price value in Redis with a 60-second expiration (TTL).
     *
     * The method is {@link Retryable} to handle transient failures and runs on
     * Java 21 Virtual Threads via {@link ExecuteOn}.
     *
     * @param priceUpdate the price update data received from Kafka
     */
    @Topic("market-prices")
    @Retryable(attempts = "3", delay = "500ms")
    @ExecuteOn(TaskExecutors.IO)
    public void onPriceTick(PriceUpdateDto priceUpdate) {
        try (var ignored = MDC.putCloseable(CORRELATION_ID_KEY, priceUpdate.eventId().toString())) {
            log.info("Received price tick for {}", priceUpdate.symbol());
            String key = STOCK_PRICE + priceUpdate.symbol();
            String value = String.valueOf(priceUpdate.price());

            RedisCommands commands = connection.sync();
            commands.setex(key, 60, value);
        }
    }
}