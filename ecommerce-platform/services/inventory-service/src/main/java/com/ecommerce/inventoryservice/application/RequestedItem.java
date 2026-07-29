package com.ecommerce.inventoryservice.application;

import java.util.UUID;

public record RequestedItem(UUID productId, int quantity) {
}
