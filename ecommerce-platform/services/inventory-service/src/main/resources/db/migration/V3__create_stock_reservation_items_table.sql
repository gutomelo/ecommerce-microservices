CREATE TABLE stock_reservation_items (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL REFERENCES stock_reservations (id) ON DELETE CASCADE,
    product_id     UUID NOT NULL,
    quantity       INTEGER NOT NULL
);

CREATE INDEX idx_stock_reservation_items_reservation_id ON stock_reservation_items (reservation_id);
