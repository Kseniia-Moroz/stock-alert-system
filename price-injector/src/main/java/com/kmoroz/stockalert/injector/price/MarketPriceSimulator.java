package com.kmoroz.stockalert.injector.price;

import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static java.util.UUID.randomUUID;

/**
 * Simulates real-time stock market price movements for a predefined set of stock symbols.
 *
 * This class is a Micronaut {@link Context} bean, meaning it is initialized and started
 * as soon as the application context is ready. It uses a {@link ScheduledExecutorService}
 * backed by Java 21 Virtual Threads to periodically update prices and broadcast them via {@link PriceProducer}.
 *
 * @author kmoroz
 */
@Context
@Slf4j
public class MarketPriceSimulator {

    private final PriceProducer priceProducer;
    private final ScheduledExecutorService scheduler;
    private final long delayMs;

    // Store the "current" state of each stock
    private final Map<String, BigDecimal> prices = new HashMap<>() {{
        put("TSLA", BigDecimal.valueOf(400.00));
        put("ORCL", BigDecimal.valueOf(150.00));
        put("AMZN", BigDecimal.valueOf(230.00));
    }};

    public MarketPriceSimulator(PriceProducer priceProducer,
                                @Value("${market.simulator.delay:2000}") long delayMs) {
        this.priceProducer = priceProducer;
        this.delayMs = delayMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().name("price-simulator-", 0).factory()
        );
    }

    /**
     * Initializes and starts the price simulation scheduler.
     * Invoked automatically by Micronaut after the bean's construction.
     */
    @PostConstruct
    public void start() {
        log.info("Starting Market Price Simulator with {}ms delay", delayMs);
        scheduler.scheduleWithFixedDelay(this::simulatePrices, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Executes a single iteration of the price simulation.
     * For each tracked stock symbol, it applies a random price movement,
     * updates the internal state, and broadcasts the new price.
     */
     void simulatePrices() {
        try {
            prices.forEach((symbol, currentPrice) -> {
                BigDecimal newPrice = applyRandomMove(currentPrice);

                // Update the map for the next iteration
                prices.put(symbol, newPrice);

                PriceUpdateDto update = PriceUpdateDto.builder()
                        .eventId(randomUUID())
                        .symbol(symbol)
                        .price(newPrice)
                        .timestamp(Instant.now())
                        .build();
                log.info(">>> Market Move: {} is now ${}", symbol, newPrice);
                
                log.info(">>> Sending Price Update: {} | CorrelationID: {}", update, update.eventId());
                priceProducer.sendPrice(symbol, update);
            });
        } catch (Exception e) {
            log.error("Price Simulation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculates a new price by applying a random percentage move (-1.5% to +1.5%) to the current price.
     *
     * @param price the current market price
     * @return the newly calculated price, scaled to 2 decimal places
     */
    private BigDecimal applyRandomMove(BigDecimal price) {
        double percent = ThreadLocalRandom.current().nextDouble(-0.015, 0.015);
        return price.multiply(BigDecimal.valueOf(1 + percent))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Shuts down the simulation scheduler gracefully before the bean is destroyed.
     */
    @PreDestroy
    public void stopRelay() {
        log.info("Shutting down OutboxRelay scheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
