package com.kmoroz.stockalert.alert.engine;

import com.kmoroz.stockalert.alert.repository.ProcessedEventRepository;
import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceConsumerTest {

    @Mock
    private PriceAlertService priceAlertService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private PriceConsumer priceConsumer;

    @BeforeEach
    void setUp() {
        priceConsumer = new PriceConsumer(priceAlertService, processedEventRepository);
    }

    @Test
    void receive_shouldProcessAlerts_whenEventIsNew() {
        UUID eventId = UUID.randomUUID();
        PriceUpdateDto update = PriceUpdateDto.builder()
                .eventId(eventId)
                .symbol("AAPL")
                .price(new BigDecimal("150.00"))
                .timestamp(Instant.now())
                .build();

        when(processedEventRepository.tryInsert(eq(eventId), eq("alert-engine"))).thenReturn(1);

        priceConsumer.receive(update);

        verify(processedEventRepository).tryInsert(eventId, "alert-engine");
        verify(priceAlertService).process(update);
    }

    @Test
    void receive_shouldIgnoreEvent_whenEventIsDuplicate() {
        UUID eventId = UUID.randomUUID();
        PriceUpdateDto update = PriceUpdateDto.builder()
                .eventId(eventId)
                .symbol("AAPL")
                .price(new BigDecimal("150.00"))
                .timestamp(Instant.now())
                .build();
        
        when(processedEventRepository.tryInsert(eq(eventId), eq("alert-engine"))).thenReturn(0);

        priceConsumer.receive(update);

        verify(processedEventRepository).tryInsert(eventId, "alert-engine");
        verify(priceAlertService, never()).process(any());
    }
}
