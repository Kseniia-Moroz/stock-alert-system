package com.kmoroz.stockalert.injector.price;

import com.kmoroz.stockalert.common.dto.PriceUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarketPriceSimulatorTest {

    @Mock
    private PriceProducer priceProducer;

    private MarketPriceSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new MarketPriceSimulator(priceProducer, 1000L);
    }

    @Test
    void simulatePrices_shouldSendUpdatesForAllSymbols() {
        simulator.simulatePrices();

        verify(priceProducer, times(3)).sendPrice(anyString(), any(PriceUpdateDto.class));
        verify(priceProducer, times(1)).sendPrice(eq("TSLA"), any(PriceUpdateDto.class));
        verify(priceProducer, times(1)).sendPrice(eq("ORCL"), any(PriceUpdateDto.class));
        verify(priceProducer, times(1)).sendPrice(eq("AMZN"), any(PriceUpdateDto.class));
    }

    @Test
    void simulatePrices_shouldGenerateValidPriceUpdates() {
        ArgumentCaptor<PriceUpdateDto> captor = ArgumentCaptor.forClass(PriceUpdateDto.class);

        simulator.simulatePrices();
        verify(priceProducer, atLeastOnce()).sendPrice(anyString(), captor.capture());
        
        PriceUpdateDto capturedUpdate = captor.getValue();
        assertNotNull(capturedUpdate.symbol());
        assertNotNull(capturedUpdate.price());
        assertNotNull(capturedUpdate.eventId());
        assertNotNull(capturedUpdate.timestamp());
    }
}
