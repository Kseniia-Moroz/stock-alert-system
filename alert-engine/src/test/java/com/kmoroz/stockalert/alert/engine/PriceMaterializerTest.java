package com.kmoroz.stockalert.alert.engine;

import com.kmoroz.stockalert.common.AlertSystemConstants;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
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
    private RedisCommands<String, String> syncCommands;

    private PriceMaterializer priceMaterializer;

    @BeforeEach
    void setUp() {
        when(connection.sync()).thenReturn(syncCommands);
        priceMaterializer = new PriceMaterializer(connection);
    }

    @Test
    void onPriceTick_shouldSetPriceInRedisWithExpiration() {
        String symbol = "AAPL";
        PriceUpdateDto priceUpdate = PriceUpdateDto.builder()
                .eventId(UUID.randomUUID())
                .symbol(symbol)
                .price(new BigDecimal("150.50"))
                .timestamp(Instant.now())
                .build();

        String expectedKey = AlertSystemConstants.STOCK_PRICE + symbol;
        String expectedValue = "150.50";

        priceMaterializer.onPriceTick(priceUpdate);

        verify(syncCommands).setex(expectedKey, 60, expectedValue);
        verify(connection, atLeastOnce()).sync();
    }
}
