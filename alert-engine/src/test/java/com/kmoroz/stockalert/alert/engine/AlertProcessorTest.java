package com.kmoroz.stockalert.alert.engine;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.kmoroz.stockalert.alert.entity.Alert;
import com.kmoroz.stockalert.alert.entity.OutboxEvent;
import com.kmoroz.stockalert.alert.repository.AlertRepository;
import com.kmoroz.stockalert.alert.repository.OutboxRepository;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import com.kmoroz.stockalert.common.enums.AlertCondition;
import com.kmoroz.stockalert.common.enums.AlertStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertProcessorTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private MeterRegistry meterRegistry;

    private AlertProcessor alertProcessor;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        alertProcessor = new AlertProcessor(alertRepository, outboxRepository, meterRegistry);
    }

    @Test
    void process_shouldDoNothing_whenNoAlertsTriggered() {
        PriceUpdateDto priceUpdate = new PriceUpdateDto(UUID.randomUUID(), "AAPL", new BigDecimal("150.00"), Instant.now());
        when(alertRepository.findTriggeredAlerts("AAPL", new BigDecimal("150.00"))).thenReturn(Collections.emptyList());

        alertProcessor.process(priceUpdate);

        verify(alertRepository, never()).saveAll(anyList());
        verify(outboxRepository, never()).saveAll(anyList());
    }

    @Test
    void process_shouldTriggerAlertsAndCreateOutboxEvents() {
        String symbol = "TSLA";
        BigDecimal price = new BigDecimal("410.00");
        PriceUpdateDto priceUpdate = new PriceUpdateDto(UUID.randomUUID(), symbol, price, Instant.now());

        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setUserId("user-1");
        alert.setSymbol(symbol);
        alert.setTargetPrice(new BigDecimal("400.00"));
        alert.setCondition(AlertCondition.ABOVE);
        alert.setStatus(AlertStatus.PENDING);

        when(alertRepository.findTriggeredAlerts(symbol, price)).thenReturn(List.of(alert));


        alertProcessor.process(priceUpdate);

        // Verify Alert was updated to TRIGGERED
        ArgumentCaptor<List<Alert>> alertCaptor = ArgumentCaptor.forClass(List.class);
        verify(alertRepository).saveAll(alertCaptor.capture());
        Alert savedAlert = alertCaptor.getValue().get(0);
        assertEquals(AlertStatus.TRIGGERED, savedAlert.getStatus());

        // Verify OutboxEvent was created
        ArgumentCaptor<List<OutboxEvent>> outboxCaptor = ArgumentCaptor.forClass(List.class);
        verify(outboxRepository).saveAll(outboxCaptor.capture());
        OutboxEvent savedEvent = outboxCaptor.getValue().get(0);
        assertEquals("user-1", savedEvent.getAggregateId());
        assertEquals(AlertStatus.TRIGGERED.name(), savedEvent.getType());
        assertTrue(savedEvent.getPayload().contains("TSLA dropped down 410.00"));
    }

    @Test
    void process_shouldHandleBelowConditionCorrectly() {
        String symbol = "ORCL";
        BigDecimal price = new BigDecimal("140.00");
        PriceUpdateDto priceUpdate = new PriceUpdateDto(UUID.randomUUID(), symbol, price, Instant.now());

        Alert alert = new Alert();
        alert.setUserId("user-2");
        alert.setSymbol(symbol);
        alert.setTargetPrice(new BigDecimal("150.00"));
        alert.setCondition(AlertCondition.BELOW);
        alert.setStatus(AlertStatus.PENDING);

        when(alertRepository.findTriggeredAlerts(symbol, price)).thenReturn(List.of(alert));

        alertProcessor.process(priceUpdate);

        ArgumentCaptor<List<OutboxEvent>> outboxCaptor = ArgumentCaptor.forClass(List.class);
        verify(outboxRepository).saveAll(outboxCaptor.capture());
        OutboxEvent savedEvent = outboxCaptor.getValue().get(0);
        assertTrue(savedEvent.getPayload().contains("ORCL dropped down 140.00"));
    }
}
