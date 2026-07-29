package com.ecommerce.inventoryservice.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StockReservationTest {

    @Test
    void createBuildsReservationWithProvidedItems() {
        UUID orderId = UUID.randomUUID();
        var items = List.of(new StockReservation.ReservedItem(UUID.randomUUID(), 2));

        StockReservation reservation = StockReservation.create(orderId, items);

        assertThat(reservation.getOrderId()).isEqualTo(orderId);
        assertThat(reservation.getItems()).isEqualTo(items);
    }
}
