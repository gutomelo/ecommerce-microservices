package com.ecommerce.inventoryservice.domain.port;

import com.ecommerce.inventoryservice.domain.StockReservation;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository {

    StockReservation save(StockReservation reservation);

    Optional<StockReservation> findByOrderId(UUID orderId);
}
