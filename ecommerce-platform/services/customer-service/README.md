# customer-service

CRUD de clientes. Não participa da Saga — é consultado apenas via API, nunca por evento.

## Papel na arquitetura

Serviço de suporte simples: cadastro de clientes, sem lógica de negócio distribuída. `order-service` recebe um `customerId` (UUID) ao criar um pedido, mas **nunca** consulta este serviço para validar que o cliente existe — não há chamada síncrona entre microsserviços na plataforma (ver [`.claude/rules/arquitetura.md`](../../.claude/rules/arquitetura.md)); a validação de que o `customerId` é real fica a cargo de quem chama a API (tipicamente o próprio cliente autenticado).

## Endpoints

| Método | Path | Papel exigido | Descrição |
|---|---|---|---|
| `POST` | `/customers` | `ADMIN` | Cria cliente |
| `GET` | `/customers/{id}` | `ADMIN`, `CUSTOMER` | Busca por id |
| `GET` | `/customers` | `ADMIN`, `CUSTOMER` | Lista paginada |
| `PUT` | `/customers/{id}` | `ADMIN` | Atualiza nome/telefone |
| `DELETE` | `/customers/{id}` | `ADMIN` | Remove |

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `customers` | `id`, `name`, `email` (único), `phone`, `active`, `created_at`, `updated_at` |

## Configuração

| Propriedade | Default local |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5434` / `customer_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `customer` / `customer` |

## Como rodar isoladamente

```bash
docker compose up -d postgres-customer config-server customer-service
```

Porta: **8082**. Swagger UI: `http://localhost:8082/swagger-ui.html`.

## Exemplos (curl, via gateway)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@ecommerce-platform.local","password":"Admin@12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# criar cliente
curl -s -X POST http://localhost:8080/customers -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Doe","email":"jane@example.com","phone":"11999999999"}'

# buscar por id
curl -s http://localhost:8080/customers/<id> -H "Authorization: Bearer $TOKEN"

# listar (paginado)
curl -s "http://localhost:8080/customers?page=0&size=20" -H "Authorization: Bearer $TOKEN"

# atualizar
curl -s -X PUT http://localhost:8080/customers/<id> -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"name":"Jane Smith","phone":"11888888888"}'

# remover
curl -s -X DELETE http://localhost:8080/customers/<id> -H "Authorization: Bearer $TOKEN"
```

## Testes

- `CustomerCrudFlowIT.java` — CRUD completo contra PostgreSQL real (Testcontainers), incluindo verificação de que `CUSTOMER` não pode escrever (403) e requisição sem token é rejeitada.
- `CustomerServiceTest.java`, `CustomerTest.java` — casos de uso e regras de domínio.
