# Especificações OpenAPI

Specs OpenAPI 3 exportadas de `/v3/api-docs` de cada serviço com API REST própria (com o ambiente local rodando - ver [`docs/deployment/local.md`](../deployment/local.md)). `gateway-service` e `config-server` não têm domínio próprio; `notification-service` não expõe API (só consome eventos) - nenhum dos três gera spec.

| Serviço | Arquivo | Endpoints |
|---|---|---|
| `auth-service` | [`auth-service-openapi.json`](auth-service-openapi.json) | `/auth/register`, `/auth/login`, `/auth/refresh` |
| `customer-service` | [`customer-service-openapi.json`](customer-service-openapi.json) | `/customers`, `/customers/{id}` |
| `product-service` | [`product-service-openapi.json`](product-service-openapi.json) | `/products`, `/products/{id}` |
| `order-service` | [`order-service-openapi.json`](order-service-openapi.json) | `/orders`, `/orders/{id}` |
| `inventory-service` | [`inventory-service-openapi.json`](inventory-service-openapi.json) | `/stock/{productId}` (somente leitura) |
| `payment-service` | [`payment-service-openapi.json`](payment-service-openapi.json) | `/payments/order/{orderId}` (somente leitura) |

## Como regenerar

Essas specs são um retrato do contrato no momento em que foram exportadas — para gerar de novo após alterar um controller:

```bash
# com o ambiente local rodando (docker compose up -d)
curl -s http://localhost:<porta-do-servico>/v3/api-docs | python3 -m json.tool > docs/api/<servico>-openapi.json
```

Cada serviço também expõe Swagger UI interativo em `http://localhost:<porta>/swagger-ui.html` enquanto estiver rodando (ver portas em [`docs/deployment/local.md`](../deployment/local.md)).

`inventory-service` e `payment-service` só expõem leitura: seus dados são criados exclusivamente por eventos consumidos da Saga (ver [`docs/events/catalogo-eventos.md`](../events/catalogo-eventos.md)), nunca via API.
