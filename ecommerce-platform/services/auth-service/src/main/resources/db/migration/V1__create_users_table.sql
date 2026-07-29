CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

-- Usuario admin de demonstracao para portfolio/testes manuais.
-- Senha: Admin@12345 (hash bcrypt gerado com BCryptPasswordEncoder, cost 10).
-- Nunca use um seed de senha fixa como este em um ambiente de producao real.
INSERT INTO users (email, password_hash, role, active, created_at, updated_at)
VALUES (
    'admin@ecommerce-platform.local',
    '$2a$10$JV.C01mFmjj1VZKHLIynvuLljCbLe23UpnRyMAUqXbmAeqzy1cJrS',
    'ADMIN',
    TRUE,
    now(),
    now()
);
