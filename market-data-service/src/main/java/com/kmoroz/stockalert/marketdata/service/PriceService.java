package com.kmoroz.stockalert.marketdata.service;

import io.lettuce.core.api.StatefulRedisConnection;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.kmoroz.stockalert.common.AlertSystemConstants.STOCK_PRICE;

/**
 * Service for retrieving real-time stock prices from a Redis data store.
 *
 * This class is a Micronaut {@link Singleton}, providing efficient access to the latest market data
 * updated by other services (e.g., the price injector). It uses {@link StatefulRedisConnection}
 * for synchronous data retrieval.
 *
 * @author kmoroz
 */
@Singleton
@Slf4j
public class PriceService {

    private final StatefulRedisConnection<String, String> redis;

    public PriceService(StatefulRedisConnection<String, String> redis) {
        this.redis = redis;
    }

    /**
     * Retrieves the latest price for a given stock symbol.
     *
     * @param symbol the stock ticker symbol (e.g., "TSLA", "AMZN")
     * @return an {@link Optional} containing the latest price as a String if found, or empty if not
     */
    public Optional<String> getLatestPrice(String symbol) {
        log.info("Getting latest price for symbol: {}", symbol);
        String result = redis.sync().get(STOCK_PRICE + symbol);
        return Optional.ofNullable(result);
    }
}
