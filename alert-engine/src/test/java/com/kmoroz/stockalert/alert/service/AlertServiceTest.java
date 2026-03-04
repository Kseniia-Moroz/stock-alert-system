package com.kmoroz.stockalert.alert.service;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import com.kmoroz.stockalert.alert.entity.Alert;
import com.kmoroz.stockalert.alert.repository.AlertRepository;
import com.kmoroz.stockalert.common.dto.AlertDto;
import com.kmoroz.stockalert.common.dto.AlertSaveDto;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository);
    }

    @Test
    void createAlert_savesAlertAndReturnsId() {
        AlertSaveDto dto = new AlertSaveDto("user1", "AAPL", new BigDecimal("150.00"), AlertCondition.ABOVE);
        UUID expectedId = UUID.randomUUID();
        
        Alert savedAlert = new Alert();
        savedAlert.setId(expectedId);
        
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);

        UUID actualId = alertService.createAlert(dto);
        assertEquals(expectedId, actualId);
        
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        
        Alert capturedAlert = alertCaptor.getValue();
        assertEquals("user1", capturedAlert.getUserId());
        assertEquals("AAPL", capturedAlert.getSymbol());
        assertEquals(new BigDecimal("150.00"), capturedAlert.getTargetPrice());
        assertEquals(AlertCondition.ABOVE, capturedAlert.getCondition());
    }

    @Test
    void getAlertHistory_returnsMappedPage() {
        String userId = "user1";
        Pageable pageable = Pageable.from(0, 10);
        
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setUserId(userId);
        alert.setSymbol("TSLA");
        alert.setTargetPrice(new BigDecimal("200.00"));
        alert.setCondition(AlertCondition.BELOW);
        alert.setStatus(AlertStatus.TRIGGERED);
        alert.setCreatedAt(Instant.now());

        Page<Alert> alertPage = Page.of(List.of(alert), pageable, 1);
        when(alertRepository.findByUserIdAndStatusOrderByCreatedAtDesc(eq(userId), eq(AlertStatus.TRIGGERED), any(Pageable.class)))
                .thenReturn(alertPage);

        Page<AlertDto> result = alertService.getAlertHistory(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        
        AlertDto dto = result.getContent().get(0);
        assertEquals(alert.getId(), dto.id());
        assertEquals(alert.getSymbol(), dto.symbol());
        assertEquals(alert.getTargetPrice(), dto.targetPrice());
        assertEquals(alert.getCondition(), dto.condition());
        
        verify(alertRepository).findByUserIdAndStatusOrderByCreatedAtDesc(userId, AlertStatus.TRIGGERED, pageable);
    }
}
