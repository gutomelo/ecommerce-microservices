# ecommerce-microservices

Plataforma de e-commerce baseada em microsserviços, orientada a eventos (Event-Driven Architecture), construída como referência de boas práticas do ecossistema Spring para portfólio técnico.

Backend distribuído de e-commerce com **9 microsserviços autônomos**, coordenados por uma **Saga por Coreografia** sobre **AWS SNS/SQS** (simulados localmente via **LocalStack**) — sem orquestrador central. Segue **DDD**, **Clean/Hexagonal Architecture** e os **Twelve-Factor App**.

> O código do projeto (Maven multi-módulo, serviços, docs) vive todo dentro de [`ecommerce-platform/`](ecommerce-platform/). Só `.github/workflows/` fica na raiz do repositório git (aqui), porque é onde o GitHub Actions exige que esteja.

## Stack

Java 21 · Spring Boot 3 · Spring Cloud (Config, Gateway) · Spring Cloud AWS (SNS/SQS) · Spring Data JPA · Spring Security + JWT · PostgreSQL · Flyway · Docker / Docker Compose · LocalStack · Resilience4j (Retry, Circuit Breaker, Rate Limiter) · Micrometer · OpenTelemetry · Prometheus · Grafana · Jaeger · Mailpit · JUnit 5 · Mockito · Testcontainers · JaCoCo.

## Arquitetura

```text
Cliente/Admin → gateway-service → { auth, customer, product, order, inventory, payment }-service
                                                              │
                                          order-service ──► SNS/SQS ◄── inventory-service
                                                              ▲              │
                                                              └── payment-service
                                                                       │
                                                          notification-service (assina tudo)
```

- **Saga por Coreografia**: `order-service` cria o pedido (`PENDING`) e publica `OrderCreated`. `inventory-service` reserva estoque e publica `StockReserved`/`StockUnavailable`. `payment-service` decide aprovar/recusar e publica `PaymentApproved`/`PaymentDeclined`. `order-service` reage a esses dois últimos e fecha o pedido (`CONFIRMED`/`CANCELLED`, com `inventory-service` liberando o estoque na compensação). `notification-service` observa tudo e envia e-mail/SMS.
- **Outbox Pattern** em todo serviço publicador (a mudança de estado e o registro do evento a publicar são commitados na mesma transação; um worker assíncrono publica de fato no SNS).
- **Idempotent Consumer** em todo serviço consumidor (tabela `processed_events` — reentrega do SQS nunca duplica efeito).
- **Retry + Circuit Breaker + DLQ**: publicação no SNS protegida por Resilience4j (retry com backoff + circuit breaker); toda fila SQS tem DLQ própria com `maxReceiveCount` configurado.
- Módulo `platform/` compartilha infraestrutura (eventos, mensageria, segurança, observabilidade, exceções, testes) sem nenhuma regra de negócio.

Diagramas C4, fluxo completo da Saga, catálogo de eventos e ADRs: ver [`ecommerce-platform/docs/`](ecommerce-platform/docs/) (índice abaixo).

## Como rodar

```bash
cd ecommerce-platform
./mvnw clean package -DskipTests
docker compose build
docker compose up -d
```

Sobe sozinho: LocalStack (com os 7 tópicos SNS e 4 filas SQS/DLQ já criados), 7 PostgreSQL, `config-server`, `gateway-service`, os 7 microsserviços de negócio, Prometheus, Grafana, Jaeger e Mailpit. Um usuário `ADMIN` já vem semeado (`admin@ecommerce-platform.local` / `Admin@12345`).

Guia completo (fluxo de fumaça via `curl`, portas de cada serviço, como acessar Grafana/Jaeger/Mailpit): **[`ecommerce-platform/docs/deployment/local.md`](ecommerce-platform/docs/deployment/local.md)**.

## Testes

```bash
cd ecommerce-platform
./mvnw clean verify
```

Testes unitários (JUnit 5 + Mockito) e de integração reais (Testcontainers: PostgreSQL e LocalStack de verdade — nunca mocks para infraestrutura) em todos os módulos. Gate de cobertura JaCoCo **≥ 80%** por módulo, obrigatório para o build passar. CI: [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## Microsserviços

| Serviço | Responsabilidade | Escuta | Publica |
|---|---|---|---|
| [`gateway-service`](ecommerce-platform/services/gateway-service/README.md) | Roteamento, JWT, rate limiting | — | — |
| [`config-server`](ecommerce-platform/services/config-server/README.md) | Configuração centralizada | — | — |
| [`auth-service`](ecommerce-platform/services/auth-service/README.md) | Login, cadastro, JWT, refresh token | — | — |
| [`customer-service`](ecommerce-platform/services/customer-service/README.md) | CRUD de clientes | — | — |
| [`product-service`](ecommerce-platform/services/product-service/README.md) | CRUD de produtos | — | — |
| [`order-service`](ecommerce-platform/services/order-service/README.md) | Cria pedidos, núcleo da Saga | `PaymentApproved`, `PaymentDeclined`, `StockUnavailable` | `OrderCreated`, `OrderConfirmed`, `OrderCancelled` |
| [`inventory-service`](ecommerce-platform/services/inventory-service/README.md) | Reserva/libera estoque | `OrderCreated`, `OrderCancelled` | `StockReserved`, `StockUnavailable` |
| [`payment-service`](ecommerce-platform/services/payment-service/README.md) | Processa pagamento (simulado, por limite) | `StockReserved` | `PaymentApproved`, `PaymentDeclined` |
| [`notification-service`](ecommerce-platform/services/notification-service/README.md) | E-mail (Mailpit) / SMS (log) | todos os 7 eventos | — |

## Módulo `platform/`

Bibliotecas Maven compartilhadas, sem regra de negócio — cada serviço acima depende só do que precisa.

| Módulo | Responsabilidade |
|---|---|
| [`platform-bom`](ecommerce-platform/platform/platform-bom/README.md) | BOM: centraliza toda versão de dependência |
| [`platform-common`](ecommerce-platform/platform/platform-common/README.md) | `ApiResponse`/`PageResponse`/`ErrorResponse`, superclasses JPA, utilitários |
| [`platform-events`](ecommerce-platform/platform/platform-events/README.md) | Contratos dos 7 eventos de domínio da Saga |
| [`platform-exception`](ecommerce-platform/platform/platform-exception/README.md) | Exceções de negócio + handler global de erro |
| [`platform-security`](ecommerce-platform/platform/platform-security/README.md) | JWT: geração, validação, filtro, roles |
| [`platform-messaging`](ecommerce-platform/platform/platform-messaging/README.md) | Publish/consume SNS/SQS, Outbox, Idempotent Consumer, Retry + Circuit Breaker |
| [`platform-observability`](ecommerce-platform/platform/platform-observability/README.md) | Correlation ID, tracing, métricas, log estruturado |
| [`platform-testing`](ecommerce-platform/platform/platform-testing/README.md) | Fixtures de evento, JWT de teste, bases de Testcontainers |

## Estrutura do repositório

```text
ecommerce-microservices/          # raiz do repositório git
├── .github/workflows/ci.yml      # CI (só pode ficar na raiz do repo, exigência do GitHub Actions)
└── ecommerce-platform/           # o projeto em si
    ├── platform/                 # bibliotecas Maven compartilhadas (sem regra de negócio)
    ├── services/                 # os 9 microsserviços, cada um com seu README
    ├── infrastructure/           # LocalStack init, config-repo, Prometheus, Grafana
    ├── docs/                     # arquitetura, diagramas, eventos, saga, API, deployment, ADRs
    ├── docker-compose.yml
    ├── pom.xml                   # Maven Multi-Module raiz
    └── CLAUDE.md                 # contexto para desenvolvimento assistido por IA
```

## Documentação

- [`ecommerce-platform/CLAUDE.md`](ecommerce-platform/CLAUDE.md) — visão geral para desenvolvimento assistido por IA (stack, regras, estrutura).
- [`ecommerce-platform/docs/architecture/visao-geral.md`](ecommerce-platform/docs/architecture/visao-geral.md) — arquitetura detalhada.
- [`ecommerce-platform/docs/diagrams/`](ecommerce-platform/docs/diagrams/) — diagramas C4 (contexto e contêineres).
- [`ecommerce-platform/docs/saga/fluxo-saga.md`](ecommerce-platform/docs/saga/fluxo-saga.md) — fluxo completo da Saga (sucesso e compensação).
- [`ecommerce-platform/docs/events/catalogo-eventos.md`](ecommerce-platform/docs/events/catalogo-eventos.md) — catálogo de eventos, tópicos e filas.
- [`ecommerce-platform/docs/api/`](ecommerce-platform/docs/api/) — especificações OpenAPI de cada serviço.
- [`ecommerce-platform/docs/deployment/local.md`](ecommerce-platform/docs/deployment/local.md) — guia de execução local.
- [`ecommerce-platform/docs/deployment/aws.md`](ecommerce-platform/docs/deployment/aws.md) — notas de migração para AWS real.
- [`ecommerce-platform/docs/decisions/`](ecommerce-platform/docs/decisions/) — Architecture Decision Records.
