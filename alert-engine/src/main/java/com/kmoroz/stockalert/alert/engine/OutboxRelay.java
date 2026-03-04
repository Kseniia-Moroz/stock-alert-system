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

@Singleton
@Slf4j
public class OutboxRelay {

    public static final int EVENTS_NUMBER_LIMIT = 100;
    private final OutboxRepository repository;
    private final NotificationProducer producer;
    private final String instanceId;
    private final long delayMs;
    private final ScheduledExecutorService scheduler;

    public OutboxRelay(OutboxRepository repository, NotificationProducer producer,
                       @Value("${INSTANCE_ID:local-instance}") String instanceId,
                       @Value("${outbox.relay.delay:1000}") long delayMs) {
        this.repository = repository;
        this.producer = producer;
        this.instanceId = instanceId;
        this.delayMs = delayMs;

        // Java 21: Using a scheduled executor that spawns Virtual Threads
        // if you want, but for a single recurring task, a single-threaded
        // scheduler is safer and more predictable.
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    }

    @PostConstruct
    public void startRelay() {
        log.info("Starting Native Java OutboxRelay for instance {} with delay {}ms", instanceId, delayMs);

        // scheduleWithFixedDelay ensures the next execution starts only
        // AFTER the previous one finishes (preventing overlapping runs).
        scheduler.scheduleWithFixedDelay(
                this::publishEvents,
                0,
                delayMs,
                TimeUnit.MILLISECONDS
        );
    }

    @Transactional
    void publishEvents() {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(1, ChronoUnit.MINUTES);

        // Atomically claim exclusive rights to these 100 events
        List<OutboxEvent> myWork = repository.lockAndFetch(instanceId, now, staleBefore, EVENTS_NUMBER_LIMIT);

        if (myWork.isEmpty()) return;

        log.info("Pod {} claimed {} events for relay", instanceId, myWork.size());

        for (OutboxEvent event : myWork) {
            try {
                // We are the exclusive owner of this row now
                //relay to kafka
                producer.sendNotification(event.getAggregateId(), event.getPayload());

                // update local state
                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                // No version collision here because we locked the row in the DB
                repository.update(event);

                log.info("Relayed event {} via CorrelationID: {}", event.getAggregateId(), event.getCorrelationId());
            } catch (Exception e) {
                log.error("Failed relay for event {}: {}", event.getId(), e.getMessage());
                // If it fails, the row remains locked by us until 'staleBefore' passes,
                // protecting other pods from trying the same failing record immediately.
            }
        }
    }

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
