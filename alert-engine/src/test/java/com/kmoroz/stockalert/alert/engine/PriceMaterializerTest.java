package com.kmoroz.stockalert.alert.engine;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.json.JsonMapper;
import com.kmoroz.stockalert.common.AlertSystemConstants;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceMaterializerTest {

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private RedisCommands<String, String> syncCommands;

    private PriceMaterializer priceMaterializer;

    @BeforeEach
    void setUp() {
        when(connection.sync()).thenReturn(syncCommands);
        priceMaterializer = new PriceMaterializer(connection,  jsonMapper);
    }

    @Test
    void onPriceTick_shouldSetPriceInRedisWithExpiration() {
        UUID eventId = UUID.randomUUID();
        String symbol = "AAPL";
        BigDecimal price = new BigDecimal("150.50");
        Instant timestamp = Instant.now();
        PriceUpdateDto priceUpdate = new PriceUpdateDto(eventId, symbol, price, timestamp);

        String expectedKey = AlertSystemConstants.STOCK_PRICE + symbol;
        String expectedValue = "150.50";

        priceMaterializer.onPriceTick(priceUpdate);

        verify(syncCommands).setex(expectedKey, 60, expectedValue);
        verify(connection, atLeastOnce()).sync();
    }
}
