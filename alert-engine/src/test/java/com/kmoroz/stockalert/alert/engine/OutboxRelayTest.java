package com.kmoroz.stockalert.alert.engine;

import com.kmoroz.stockalert.alert.entity.OutboxEvent;
import com.kmoroz.stockalert.alert.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxRepository repository;

    @Mock
    private NotificationProducer producer;

    private OutboxRelay relay;
    private final String instanceId = "test-instance";

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, producer, instanceId, 1000L);
    }

    @Test
    void publishEvents_shouldProcessLockedEvents() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setVersion(0L);
        event.setAggregateId("user-123");
        event.setCorrelationId(UUID.randomUUID().toString());
        event.setType("ALERT_TRIGGERED");
        event.setPayload("payload-json");

        when(repository.lockAndFetch(eq(instanceId), any(Instant.class), any(Instant.class), eq(OutboxRelay.EVENTS_NUMBER_LIMIT)))
                .thenReturn(List.of(event));

        relay.publishEvents();
        verify(producer).sendNotification("user-123", "payload-json");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).update(captor.capture());
        OutboxEvent updated = captor.getValue();
        assertNotNull(updated.getProcessedAt(), "processedAt must be set after successful publish");
    }

    @Test
    void publishEvents_shouldHandleEmptyWorkload() {
        when(repository.lockAndFetch(eq(instanceId), any(Instant.class), any(Instant.class), eq(OutboxRelay.EVENTS_NUMBER_LIMIT)))
                .thenReturn(List.of());

        relay.publishEvents();

        verify(producer, never()).sendNotification(anyString(), anyString());
        verify(repository, never()).update(any());
    }
}
