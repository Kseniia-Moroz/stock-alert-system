package com.kmoroz.stockalert.alert.engine;

import com.kmoroz.stockalert.alert.entity.OutboxEvent;
import com.kmoroz.stockalert.alert.repository.OutboxRepository;
import io.micronaut.context.annotation.Value;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically relays events from the database outbox table to the Kafka notification topic.
 *
 * This class implements the Transactional Outbox pattern, ensuring that event notifications are
 * reliably delivered to Kafka. It uses a {@link ScheduledExecutorService} backed by Java 21
 * Virtual Threads to periodically poll the database for pending {@link OutboxEvent} entries,
 * publish them via {@link NotificationProducer}, and mark them as processed.
 *
 * It supports high availability and prevents duplicate processing by using an optimistic locking
 * mechanism with an instance ID and timestamps.
 *
 * @author kmoroz
 */
@Singleton
@Slf4j
public class OutboxRelay {

    public static final int EVENTS_NUMBER_LIMIT = 100;
    private final OutboxRepository repository;
    private final NotificationProducer producer;
    private final String instanceId;
    private final long delayMs;
    private final ScheduledExecutorService scheduler;

    /**
     * Constructs a new OutboxRelay.
     *
     * @param repository the repository used for fetching and updating outbox events
     * @param producer the producer used to send notifications to Kafka
     * @param instanceId a unique identifier for this relay instance (e.g., pod name in K8s)
     * @param delayMs the fixed delay in milliseconds between relay iterations
     */
    public OutboxRelay(OutboxRepository repository, NotificationProducer producer,
                       @Value("${INSTANCE_ID:local-instance}") String instanceId,
                       @Value("${outbox.relay.delay:1000}") long delayMs) {
        this.repository = repository;
        this.producer = producer;
        this.instanceId = instanceId;
        this.delayMs = delayMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    }

    /**
     * Initializes and starts the relay scheduler.
     * Invoked automatically by Micronaut after the bean's construction.
     */
    @PostConstruct
    public void startRelay() {
        log.info("Starting Native Java OutboxRelay for instance {} with delay {}ms", instanceId, delayMs);
        scheduler.scheduleWithFixedDelay(
                this::publishEvents,
                0,
                delayMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Executes a single iteration of the outbox relay.
     *
     * This method:
     * 1. Locks and fetches a batch of pending or stale events from the repository.
     * 2. Iterates over the batch and publishes each event's payload to Kafka.
     * 3. Marks each successfully published event as processed in the database.
     *
     * This method is {@link Transactional} to ensure database updates are committed correctly.
     */
    @Transactional
    public void publishEvents() {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(1, ChronoUnit.MINUTES);

        List<OutboxEvent> events = repository.lockAndFetch(instanceId, now, staleBefore, EVENTS_NUMBER_LIMIT);

        if (events.isEmpty()) return;

        log.info("Pod {} claimed {} events for relay", instanceId, events.size());

        for (OutboxEvent event : events) {
            try {
                producer.sendNotification(event.getAggregateId(), event.getPayload());

                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                repository.update(event);

                log.info("Relayed event {} via CorrelationID: {}", event.getAggregateId(), event.getCorrelationId());
            } catch (Exception e) {
                log.error("Failed relay for event {}: {}", event.getId(), e.getMessage());
            }
        }
    }

    /**
     * Shuts down the relay scheduler gracefully before the bean is destroyed.
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
