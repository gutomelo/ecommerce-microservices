CREATE TABLE stock_reservations (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);
