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

@KafkaListener(groupId = "price-materializer")
@Slf4j
public class PriceMaterializer {

    private final StatefulRedisConnection<String, String> connection;
    private final JsonMapper jsonMapper;

    public PriceMaterializer(StatefulRedisConnection<String, String> connection, JsonMapper jsonMapper) {
        this.connection = connection;
        this.jsonMapper = jsonMapper;
    }

    @Topic("market-prices")
    @Retryable(attempts = "3", delay = "500ms") // Short retry for Redis
    @ExecuteOn(TaskExecutors.IO)
    public void onPriceTick(PriceUpdateDto priceUpdate) {
        try (var ignored = MDC.putCloseable(CORRELATION_ID_KEY, priceUpdate.eventId().toString())) {
            log.info("Received price tick for {}", priceUpdate.symbol());
            String key = STOCK_PRICE + priceUpdate.symbol();
            String value = String.valueOf(priceUpdate.price());

            RedisCommands commands = connection.sync();
            // 60 seconds is usually safe for a real-time feed
            commands.setex(key, 60, value);
        }
    }
}