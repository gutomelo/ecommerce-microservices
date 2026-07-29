# product-service

CRUD de produtos do catálogo. Não participa da Saga — o controle de estoque *reservável* é responsabilidade separada do `inventory-service`.

## Papel na arquitetura

Mantém o catálogo (nome, descrição, categoria, preço) e um campo `stock` — mas esse `stock` é só a quantidade de exibição do catálogo (CRUD simples), **não** o ledger usado pela Saga para reservar/liberar estoque. Esse é um dado independente, mantido pelo `inventory-service` em seu próprio banco (ver [`services/inventory-service/README.md`](../inventory-service/README.md)) — os dois nunca são sincronizados automaticamente neste projeto, por design (cada serviço é dono do seu próprio dado, ver [`.claude/rules/banco-de-dados.md`](../../.claude/rules/banco-de-dados.md)).

## Endpoints

| Método | Path | Papel exigido | Descrição |
|---|---|---|---|
| `POST` | `/products` | `ADMIN` | Cria produto |
| `GET` | `/products/{id}` | `ADMIN`, `CUSTOMER` | Busca por id |
| `GET` | `/products` | `ADMIN`, `CUSTOMER` | Lista paginada |
| `PUT` | `/products/{id}` | `ADMIN` | Atualiza produto |
| `DELETE` | `/products/{id}` | `ADMIN` | Remove |

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `products` | `id`, `name`, `description`, `category`, `price` (`NUMERIC(12,2)`), `stock` (quantidade de catálogo, default 0), `created_at`, `updated_at` |

## Configuração

| Propriedade | Default local |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5435` / `product_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `product` / `product` |

## Como rodar isoladamente

```bash
docker compose up -d postgres-product config-server product-service
```

Porta: **8083**. Swagger UI: `http://localhost:8083/swagger-ui.html`.

## Exemplos (curl, via gateway)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"email":"admin@ecommerce-platform.local","password":"Admin@12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# criar produto
curl -s -X POST http://localhost:8080/products -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Teclado mecanico","description":"Switches azuis","category":"perifericos","price":249.90,"stock":50}'

# buscar por id
curl -s http://localhost:8080/products/<id> -H "Authorization: Bearer $TOKEN"

# listar (paginado)
curl -s "http://localhost:8080/products?page=0&size=20" -H "Authorization: Bearer $TOKEN"

# atualizar
curl -s -X PUT http://localhost:8080/products/<id> -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Teclado mecanico","description":"Switches marrons","category":"perifericos","price":259.90,"stock":40}'

# remover
curl -s -X DELETE http://localhost:8080/products/<id> -H "Authorization: Bearer $TOKEN"
```

## Testes

- `ProductCrudFlowIT.java` — CRUD completo contra PostgreSQL real (Testcontainers), incluindo validação de preço inválido (400) e restrição de escrita a `ADMIN`.
- `ProductServiceTest.java`, `ProductTest.java` — casos de uso e regras de domínio.
