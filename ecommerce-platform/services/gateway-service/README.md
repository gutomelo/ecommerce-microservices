# gateway-service

Porta de entrada única da plataforma. Roteia, autentica e limita taxa de requisições — nenhum outro serviço deve ser chamado diretamente pelo cliente.

## Papel na arquitetura

Implementação de **Spring Cloud Gateway**, o único serviço **reativo** (WebFlux/Netty) do projeto — por isso não segue a estrutura hexagonal padrão (sem `domain/`/`application/`, mesma exceção do `config-server`; ver [`.claude/rules/estrutura-microsservico.md`](../../.claude/rules/estrutura-microsservico.md)) e reimplementa como `GlobalFilter` reativo o que nos demais serviços é um `OncePerRequestFilter` Servlet (`platform-security`/`platform-observability` não se aplicam diretamente aqui).

Responsabilidades, todas em `infrastructure/`:

1. **`CorrelationIdGlobalFilter`** (prioridade mais alta) — reaproveita ou gera o header `X-Correlation-Id`, propagando-o adiante para permitir rastrear uma requisição por todos os serviços/logs que ela toca.
2. **`JwtAuthenticationGlobalFilter`** — valida o JWT (reaproveitando `JwtTokenProvider` de `platform-security`, a única parte dele que não depende de Servlet) para todo caminho que não esteja na lista de rotas públicas; em caso de sucesso, injeta `X-User-Id`/`X-User-Role` na requisição repassada ao serviço de destino (que ainda valida o token de novo — defesa em profundidade, ver [`.claude/rules/seguranca.md`](../../.claude/rules/seguranca.md)).
3. **`RateLimitingGlobalFilter`** — Resilience4j `RateLimiter` em memória por IP do cliente, devolve `429` quando excedido.

## Rotas

| Path | Destino |
|---|---|
| `/auth/**` | `auth-service` (público, não exige JWT) |
| `/customers/**` | `customer-service` |
| `/products/**` | `product-service` |
| `/orders/**` | `order-service` |
| `/stock/**` | `inventory-service` |
| `/payments/**` | `payment-service` |

`notification-service` não tem rota — é consumidor de eventos puro, sem API HTTP.

## Configuração

| Propriedade | Default | Descrição |
|---|---|---|
| `gateway.security.public-paths` | `/auth/**`, `/actuator/**` | Caminhos que não exigem JWT |
| `gateway.rate-limit.limit-for-period` | `20` | Requisições permitidas por período, por IP |
| `gateway.rate-limit.limit-refresh-period-millis` | `1000` | Duração do período (ms) |

Sem banco de dados — stateless.

## Como rodar isoladamente

```bash
docker compose up -d gateway-service
```

Porta: **8080**. Todo exemplo de `curl` nos READMEs dos outros serviços passa por aqui.

## Exemplo (login + chamada autenticada, via gateway)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ecommerce-platform.local","password":"Admin@12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

curl -s http://localhost:8080/products -H "Authorization: Bearer $TOKEN"
```

Requisição sem token, ou a um path protegido sem role suficiente, retorna `401`/`403` antes mesmo de chegar ao serviço de destino.

## Testes

`CorrelationIdGlobalFilterTest.java`, `JwtAuthenticationGlobalFilterTest.java`, `RateLimitingGlobalFilterTest.java` — todos unitários (sem `*IT.java`; não há estado externo próprio para testar via Testcontainers).
