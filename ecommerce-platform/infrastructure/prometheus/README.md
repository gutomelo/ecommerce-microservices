# infrastructure/prometheus

Configuração de scrape do Prometheus — um alvo por microsserviço, todos expondo `/actuator/prometheus` graças a [`platform-observability`](../../platform/platform-observability/README.md).

## O que faz

[`prometheus.yml`](prometheus.yml) é montado direto no container (`docker-compose.yml`, volume somente-leitura) e define um `job_name` por serviço, todos com `metrics_path: /actuator/prometheus`:

| job_name | Alvo |
|---|---|
| `gateway-service` | `gateway-service:8080` |
| `config-server` | `config-server:8888` |
| `auth-service` | `auth-service:8081` |
| `customer-service` | `customer-service:8082` |
| `product-service` | `product-service:8083` |
| `order-service` | `order-service:8084` |
| `inventory-service` | `inventory-service:8085` |
| `payment-service` | `payment-service:8086` |
| `notification-service` | `notification-service:8087` |

`scrape_interval`/`evaluation_interval`: 15s.

Nenhum serviço precisa de configuração própria além disso — todos ganham o endpoint `/actuator/prometheus` só por dependerem de `platform-observability` (`spring-boot-starter-actuator` + `micrometer-registry-prometheus`).

## Como acessar

```text
http://localhost:9090
```

Consultas úteis: `up` (quais alvos estão respondendo), `http_server_requests_seconds_count` (contagem de requisições por serviço/endpoint), `jvm_memory_used_bytes`.

## Referências

- [`infrastructure/grafana/README.md`](../grafana/README.md) — Grafana já vem provisionado com este Prometheus como datasource.
- [`.claude/rules/observabilidade.md`](../../.claude/rules/observabilidade.md) — regras de observabilidade do projeto.
