package com.kmoroz.stockalert.alert.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import com.kmoroz.stockalert.alert.entity.Alert;
import com.kmoroz.stockalert.common.enums.AlertStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @Query("SELECT a FROM Alert a WHERE a.symbol = :symbol " +
            "AND a.status = 'PENDING' " +
            "AND ((a.condition = 'ABOVE' AND a.targetPrice <= :currentPrice) " +
            "  OR (a.condition = 'BELOW' AND a.targetPrice >= :currentPrice))")
    List<Alert> findPendingAlerts(String symbol, BigDecimal currentPrice);

    Page<Alert> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, AlertStatus status, Pageable pageable);
}
