# platform-testing

Builders/factories de eventos, geração de JWT de teste e bases reutilizáveis de Testcontainers. Toda dependência aqui é `compile` de propósito — mesmo padrão do `spring-boot-starter-test` — para que, ao ser importado em escopo `test` por um serviço, JUnit/Testcontainers/AssertJ cheguem juntos por transitividade.

## Papel na arquitetura

Sem `@AutoConfiguration`, sem `@ConfigurationProperties` — só classes Java puras de apoio a teste. É a única dependência `test`-scope que praticamente todo teste de integração do projeto usa.

## API pública

### `EventFixtures`

Fábrica de instâncias válidas e (pseudo-)aleatórias de cada evento do catálogo, para não repetir o boilerplate de montar payload em cada serviço. Nunca redefine um evento — só monta instâncias com as classes reais de [`platform-events`](../platform-events/README.md).

```java
EventFixtures.orderCreatedEvent()                              // correlationId aleatorio
EventFixtures.orderCreatedEvent(correlationId)
EventFixtures.stockReservedEvent(orderId, correlationId)
EventFixtures.stockUnavailableEvent(orderId, correlationId)
EventFixtures.paymentApprovedEvent(orderId, correlationId)
EventFixtures.paymentDeclinedEvent(orderId, correlationId)
EventFixtures.orderConfirmedEvent(orderId, correlationId)
EventFixtures.orderCancelledEvent(orderId, correlationId)
```

### `TestJwtTokenFactory`

Gera tokens JWT válidos sem precisar subir um `auth-service` de verdade.

```java
public static final String TEST_SECRET = "test-secret-key-with-at-least-32-characters!!";

var tokens = new TestJwtTokenFactory(); // ou new TestJwtTokenFactory(outroSegredo)
tokens.adminToken();                    // subject "admin@example.com"
tokens.customerToken("cliente@x.com");  // subject customizado
tokens.tokenProvider();                 // acesso ao JwtTokenProvider por baixo, se precisar de algo mais avançado
```

O serviço sob teste precisa configurar `platform.security.jwt.secret` com o **mesmo** valor de `TEST_SECRET` (normalmente no `application.yml` de teste) para os tokens gerados aqui validarem.

### Bases de Testcontainers (padrão *singleton container*)

Todas seguem o mesmo padrão: container `static`, iniciado uma única vez num bloco `static { ... }` (nunca parado explicitamente — o Ryuk do Testcontainers encerra ao fim da JVM), com propriedades registradas via `@DynamicPropertySource`. Isso evita que cada serviço reimplemente o ciclo de vida do container.

| Classe | Container(es) | Propriedades registradas |
|---|---|---|
| `PostgresTestContainerSupport` | `postgres:16-alpine` (db/user/pass `platform_test`) | `spring.datasource.url/username/password` |
| `LocalStackTestContainerSupport` | `localstack/localstack:3.8` (SNS+SQS) | `spring.cloud.aws.region.static`, `credentials.access-key/secret-key`, `endpoint` |
| `PostgresAndLocalStackTestContainerSupport` | os dois acima juntos | as 7 propriedades combinadas |

**Por que existe uma terceira classe combinada, em vez de só herdar as outras duas**: Java não tem herança múltipla. Serviços que precisam de Postgres **e** SNS/SQS ao mesmo tempo (o fluxo completo de outbox + idempotent consumer em `order-service`/`inventory-service`/`payment-service`/`notification-service`) estendem `PostgresAndLocalStackTestContainerSupport`.

## Como usar

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>platform-testing</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest(classes = OrderServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderFlowIT extends PostgresAndLocalStackTestContainerSupport {

    private final TestJwtTokenFactory tokenFactory = new TestJwtTokenFactory();

    @Test
    void createsOrderAndPublishesEvent() {
        String token = tokenFactory.customerToken();
        // ... chama a API real, contra Postgres e LocalStack reais
    }
}
```

## Dependências principais

`platform-events`, `platform-security` (compile — não test, de propósito, ver nota no topo), `junit-jupiter`, `spring-test`, `testcontainers` (`junit-jupiter`, `postgresql`, `localstack`), `assertj-core`.

## Testes

- `EventFixturesTest.java`, `TestJwtTokenFactoryTest.java` — unitários.
- `PostgresTestContainerSupportIT.java`, `LocalStackTestContainerSupportIT.java`, `PostgresAndLocalStackTestContainerSupportIT.java` — testes que testam a própria infraestrutura de teste (sobem o(s) container(s) de verdade e verificam que ficam acessíveis).
