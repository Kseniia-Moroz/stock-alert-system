package com.kmoroz.stockalert.marketdata.service;

import io.lettuce.core.api.StatefulRedisConnection;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.kmoroz.stockalert.common.AlertSystemConstants.STOCK_PRICE;

@Singleton
@Slf4j
public class PriceService {

    private final StatefulRedisConnection<String, String> redis;

    public PriceService(StatefulRedisConnection<String, String> redis) {
        this.redis = redis;
    }

    public Optional<String> getLatestPrice(String symbol) {
        log.info("Getting latest price for symbol: {}", symbol);
        String result = redis.sync().get(STOCK_PRICE + symbol);
        return Optional.ofNullable(result);
    }
}
