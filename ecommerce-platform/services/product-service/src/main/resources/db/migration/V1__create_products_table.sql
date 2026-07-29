CREATE TABLE products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(2000),
    category    VARCHAR(100)  NOT NULL,
    price       NUMERIC(12,2) NOT NULL,
    stock       INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL
);
