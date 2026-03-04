package com.kmoroz.stockalert.marketdata.service;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import com.kmoroz.stockalert.common.AlertSystemConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Mock
    private RedisCommands<String, String> syncCommands;

    private PriceService priceService;

    @BeforeEach
    void setUp() {
        when(connection.sync()).thenReturn(syncCommands);
        priceService = new PriceService(connection);
    }

    @Test
    void getLatestPrice_returnsValue_whenPriceExists() {
        String symbol = "AAPL";
        String expectedPrice = "150.25";
        String redisKey = AlertSystemConstants.STOCK_PRICE + symbol;
        when(syncCommands.get(redisKey)).thenReturn(expectedPrice);

        Optional<String> actualPrice = priceService.getLatestPrice(symbol);

        assertTrue(actualPrice.isPresent());
        assertEquals(expectedPrice, actualPrice.get());
        verify(syncCommands).get(redisKey);
    }

    @Test
    void getLatestPrice_returnsEmpty_whenPriceDoesNotExist() {
        String symbol = "GOOGL";
        String redisKey = AlertSystemConstants.STOCK_PRICE + symbol;
        when(syncCommands.get(redisKey)).thenReturn(null);

        Optional<String> result = priceService.getLatestPrice(symbol);
        assertTrue(result.isEmpty(), "Result should be empty when price is not found in Redis");
        verify(syncCommands).get(redisKey);
    }
}
