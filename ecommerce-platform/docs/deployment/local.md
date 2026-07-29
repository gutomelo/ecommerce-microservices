# Guia de execução local

Todo o ambiente sobe com um único `docker compose up`, sem passos manuais de configuração de infraestrutura (tópicos SNS, filas SQS e usuário admin são criados automaticamente).

## Pré-requisitos

- Docker + Docker Compose v2 (comando `docker compose`, não `docker-compose`).
- JDK 21 (só necessário se for compilar/rodar fora do Docker; o `docker compose up` sozinho não exige Java na máquina host).

## Subir o ambiente completo

```bash
cd ecommerce-platform

# build dos jars (necessario antes do primeiro docker compose build,
# pois cada Dockerfile copia um target/*-exec.jar ja compilado)
./mvnw clean package -DskipTests

# build das imagens de cada microsservico
docker compose build

# sobe tudo: LocalStack, 7 PostgreSQL, config-server, gateway, os 7
# microsservicos de negocio, Prometheus, Grafana, Jaeger e Mailpit
docker compose up -d
```

O que acontece automaticamente na subida:

- **LocalStack** roda [`infrastructure/localstack/init/init-aws.sh`](../../infrastructure/localstack/init/init-aws.sh) assim que fica pronto: cria os 7 tópicos SNS e as 4 filas SQS (+ DLQs) do [catálogo de eventos](../events/catalogo-eventos.md), já com as subscriptions corretas (fan-out da Saga).
- **auth-service** roda sua migration Flyway, que já semeia um usuário `ADMIN`:
  - e-mail: `admin@ecommerce-platform.local`
  - senha: `Admin@12345`

## Verificar que subiu corretamente

```bash
# health check de cada servico (todos expõem /actuator/health)
for port in 8080 8081 8082 8083 8084 8085 8086 8087 8888; do
  echo "porta $port:"; curl -s "http://localhost:${port}/actuator/health"; echo
done

# topicos e filas criados no LocalStack
docker exec localstack awslocal sns list-topics
docker exec localstack awslocal sqs list-queues
```

## Fluxo de fumaça completo (via Gateway, como um cliente real)

```bash
# 1. login (retorna JWT)
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ecommerce-platform.local","password":"Admin@12345"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

# 2. criar um pedido (dispara a Saga: reserva estoque -> processa pagamento -> confirma pedido)
curl -s -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"customerId":"'"$(python3 -c 'import uuid;print(uuid.uuid4())')"'","items":[{"productId":"'"$(python3 -c 'import uuid;print(uuid.uuid4())')"'","quantity":1,"unitPrice":19.90}]}'

# 3. alguns segundos depois, consultar o pedido pelo id retornado acima -
#    o status deve ter avancado de PENDING para CONFIRMED
curl -s http://localhost:8080/orders/<id-do-pedido> -H "Authorization: Bearer $TOKEN"
```

Pedidos com valor total acima de R$ 500,00 são recusados pela regra determinística simulada de `payment-service` (ver `platform.payment.approval-threshold`), disparando a compensação: `PaymentDeclined` → `OrderCancelled` → estoque liberado por `inventory-service`. Isso permite demonstrar os dois caminhos da Saga (sucesso e compensação) sem depender de nenhum estado externo.

## Rodar a suíte de testes completa

```bash
cd ecommerce-platform
./mvnw clean verify
```

Roda testes unitários (JUnit + Mockito) e de integração reais (Testcontainers: PostgreSQL e LocalStack de verdade, não mocks) de todos os módulos, e falha o build se a cobertura de qualquer módulo cair abaixo de 80% (JaCoCo).

## Acessar as ferramentas

| Ferramenta | URL | Credenciais |
|---|---|---|
| Grafana | http://localhost:3000 | `admin` / `admin` |
| Prometheus | http://localhost:9090 | — |
| Jaeger | http://localhost:16686 | — |
| Mailpit (UI de e-mails enviados) | http://localhost:8026 | — |
| Swagger UI de cada serviço com API | `http://localhost:<porta>/swagger-ui.html` | Bearer token (exceto `/auth/**`) |

## Portas de cada serviço

| Serviço | Porta | Banco (host) |
|---|---|---|
| `gateway-service` | 8080 | — |
| `auth-service` | 8081 | 5433 |
| `customer-service` | 8082 | 5434 |
| `product-service` | 8083 | 5435 |
| `order-service` | 8084 | 5436 |
| `inventory-service` | 8085 | 5437 |
| `payment-service` | 8086 | 5438 |
| `notification-service` | 8087 | 5439 |
| `config-server` | 8888 | — |
| LocalStack (SNS/SQS) | 4566 | — |

## Encerrar o ambiente

```bash
docker compose down          # para e remove os containers, mantem os volumes (dados persistem)
docker compose down -v       # tambem remove os volumes (reset completo, inclusive os bancos)
```
