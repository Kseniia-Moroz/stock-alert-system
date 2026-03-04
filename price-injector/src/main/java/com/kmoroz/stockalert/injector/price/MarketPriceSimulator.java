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

    @PostConstruct
    public void start() {
        log.info("Starting Price Simulator with {}ms delay on Virtual Thread", delayMs);
        scheduler.scheduleWithFixedDelay(this::simulatePrices, 0, delayMs, TimeUnit.MILLISECONDS);
    }

     void simulatePrices() {
        try {
            prices.forEach((symbol, currentPrice) -> {
                BigDecimal newPrice = applyRandomMove(currentPrice);

                // Update the map for the next iteration
                prices.put(symbol, newPrice);

                PriceUpdateDto update = new PriceUpdateDto(java.util.UUID.randomUUID(), symbol, newPrice, Instant.now());
                log.info(">>> Market Move: {} is now ${}", symbol, newPrice);

                // Send to Kafka (Transactional Outbox logic should be here)
                log.info(">>> Sending Price Update: {} | CorrelationID: {}", update, update.eventId());
                priceProducer.sendPrice(symbol, update);
            });
        } catch (Exception e) {
            log.error("Price Simulation failed: {}", e.getMessage(), e);
        }
    }

    private BigDecimal applyRandomMove(BigDecimal price) {
        // ThreadLocalRandom is better for performance and modern Java patterns
        // This generates a number between -0.015 (-1.5%) and 0.015 (+1.5%)
        double percent = ThreadLocalRandom.current().nextDouble(-0.015, 0.015);

        // Price * (1 + percent)
        // Example: 400 * (1 + 0.01) = 404
        return price.multiply(BigDecimal.valueOf(1 + percent))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down Price Simulator...");
        scheduler.shutdown();
    }
}
