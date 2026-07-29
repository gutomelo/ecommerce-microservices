# platform-messaging

Abstração de publicação/consumo via AWS SNS/SQS. Nenhum microsserviço fala com o SDK da AWS diretamente — tudo passa por aqui.

## Papel na arquitetura

O módulo mais complexo do `platform/`, reúne quatro responsabilidades que, sem ele, cada serviço reimplementaria de forma ligeiramente diferente: publicação resiliente no SNS, (de)serialização de eventos, roteamento de mensagens por `eventType` numa mesma fila, e o mecanismo genérico de consumo idempotente. Um serviço ganha publish **e** consume só por ter este módulo no classpath (o publisher só é criado se houver um `SnsTemplate` disponível — serviços somente-consumidores, como `payment-service`/`notification-service`, simplesmente não recebem esse bean).

## API pública

| Tipo | Descrição |
|---|---|
| `EventPublisher` (interface) | `publish(topic, BaseEvent<?>)` e `publishRaw(topic, eventType, jsonPayload)` — este último usado pelo worker do Outbox Pattern, que já tem o JSON serializado e não precisa reconstruir o objeto Java. |
| `EventConsumer<T>` (interface) | Porta implementada pela camada de aplicação de cada serviço: `consume(T event)`, já desserializado e checado quanto a idempotência. |
| `MessageSerializer` / `MessageDeserializer` (interfaces) | Abstraem Jackson — implementações `JacksonMessageSerializer`/`JacksonMessageDeserializer`. Falha vira `MessageSerializationException`. |
| `EventTypeRouter` | Roteia uma mensagem crua para o handler certo, espiando o campo `eventType` do JSON — necessário porque uma fila SQS recebe fan-out de **múltiplos** tópicos SNS (ex.: `order-queue` recebe `StockUnavailable`, `PaymentApproved` e `PaymentDeclined`). `register(eventType, handler)` (fluente) + `route(rawMessageBody)`. |
| `IdempotentEventDispatcher<T>` | Junta desserializar → checar `processed_events` → delegar ao `EventConsumer` → marcar processado, num só `dispatch(rawMessageBody)`. É o que cada `@SqsListener` de cada serviço chama. |
| `ProcessedEventChecker` (interface) | Porta do Idempotent Consumer Pattern — `isProcessed(eventId)`/`markProcessed(eventId)`. Este módulo nunca acessa banco; cada serviço implementa contra sua própria tabela `processed_events`. |
| `SnsEventPublisher` | Única implementação real de `EventPublisher`. Resilience4j Retry (backoff exponencial) **e** Circuit Breaker — ver seção dedicada abaixo. |

## Resiliência: Retry + Circuit Breaker

`SnsEventPublisher` envolve toda chamada ao SNS assim: **Circuit Breaker por fora, Retry por dentro** (`CircuitBreaker.decorateSupplier(cb, Retry.decorateSupplier(retry, chamada))`) — de propósito, nessa ordem. Cada sequência **completa** de tentativas do Retry conta como **uma única** chamada na janela do Circuit Breaker; se fosse ao contrário, cada tentativa individual do retry contaria separadamente, e o circuito abriria por causa das próprias tentativas internas do retry, não por falhas reais e sustentadas. Quando o circuito abre, a chamada seguinte falha imediatamente (sem sequer tentar a rede) até o `waitDurationInOpenState` passar.

Prova disso, contra mocks: `SnsEventPublisherTest.circuitBreakerOpensAfterRepeatedFailuresAndFailsFastWithoutCallingSns`.

## Auto-configuração

`MessagingAutoConfiguration` (`@AutoConfigureAfter(SnsAutoConfiguration.class)`, `@EnableConfigurationProperties(MessagingProperties.class)`):

| Bean | Condição |
|---|---|
| `ObjectMapper` (com `JavaTimeModule`) | `@ConditionalOnMissingBean` |
| `MessageSerializer` / `MessageDeserializer` | `@ConditionalOnMissingBean` |
| `EventPublisher` (→ `SnsEventPublisher`) | `@ConditionalOnBean(SnsTemplate.class)` **e** `@ConditionalOnMissingBean` |
| `EventTypeRouter` | `@ConditionalOnMissingBean` (cada serviço com fila multi-evento sobrepõe este bean registrando suas próprias rotas — ver `*QueueRoutingConfig` em `order-service`/`inventory-service`/`notification-service`) |

**Por que `@AutoConfigureAfter(SnsAutoConfiguration.class)`**: sem essa ordem explícita, a avaliação de autoconfigurações de jars diferentes não é garantida, e `@ConditionalOnBean(SnsTemplate.class)` podia ser avaliado **antes** do `SnsTemplate` existir — o `EventPublisher` simplesmente não era criado, silenciosamente, mesmo com `spring-cloud-aws-starter-sns` no classpath. Foi um bug real, só descoberto ao rodar o primeiro serviço publicador de verdade (não aparecia nos testes deste módulo, que registram o `SnsTemplate` manualmente).

## Configuração

`MessagingProperties`, prefixo `platform.messaging`:

| Propriedade | Default | Descrição |
|---|---|---|
| `publish-retry.max-attempts` | `3` | |
| `publish-retry.initial-interval-millis` | `200` | |
| `publish-retry.multiplier` | `2.0` | Backoff exponencial |
| `publish-circuit-breaker.sliding-window-size` | `10` | |
| `publish-circuit-breaker.minimum-number-of-calls` | `5` | |
| `publish-circuit-breaker.failure-rate-threshold` | `50` (%) | |
| `publish-circuit-breaker.wait-duration-in-open-state-millis` | `10000` | |
| `publish-circuit-breaker.permitted-number-of-calls-in-half-open-state` | `3` | |

O retry de **consumo** (lado SQS) não é configurado aqui — é `maxReceiveCount` + `visibility timeout` da própria fila, definidos em [`infrastructure/localstack/init/init-aws.sh`](../../infrastructure/localstack/init/init-aws.sh) (ver [`docs/events/catalogo-eventos.md`](../../docs/events/catalogo-eventos.md) para os valores por fila).

## Como usar

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>platform-messaging</artifactId>
</dependency>
```

Publicar (sempre via Outbox — nunca direto na transação de negócio):

```java
outboxEventStore.store(OrderCreatedEvent.of(orderId, correlationId, traceId, payload), OrderCreatedEvent.EVENT_TYPE);
// um worker separado chama depois: eventPublisher.publishRaw(topic, eventType, jsonSalvoNoOutbox)
```

Consumir (fila com um único tipo de evento — ver `payment-service`):

```java
@SqsListener("payment-queue")
public void listen(String rawMessageBody) {
    dispatcher.dispatch(rawMessageBody); // IdempotentEventDispatcher<StockReservedEvent>
}
```

Consumir (fila com múltiplos tipos de evento — ver `order-service`/`inventory-service`/`notification-service`):

```java
@SqsListener("order-queue")
public void listen(String rawMessageBody) {
    eventTypeRouter.route(rawMessageBody); // cada rota já é um IdempotentEventDispatcher::dispatch
}
```

## Dependências principais

`platform-events`, `platform-exception`, `spring-cloud-aws-starter-sns` + `spring-cloud-aws-starter-sqs`, `resilience4j-retry` + `resilience4j-circuitbreaker`, `jackson-databind`.

## Testes

- **Unitários**: `EventTypeRouterTest`, `IdempotentEventDispatcherTest`, `JacksonMessageSerializerTest`/`DeserializerTest`, `MessagingAutoConfigurationTest`, `SnsEventPublisherTest` (retry, circuit breaker).
- **Integração** (`*IT.java`, LocalStack real via Testcontainers):
  - `SnsToSqsLocalStackIT` — prova publish → SNS → fila SQS assinada (`RawMessageDelivery=true`, formato de produção) → `IdempotentEventDispatcher` consumindo, e que reenviar a mesma mensagem não duplica o efeito.
  - `DlqRedeliveryIT` — prova a regra "mensagem que esgota `maxReceiveCount` vai para a DLQ", com uma fila própria de teste (`maxReceiveCount=2`, `visibilityTimeout=1s` para rodar rápido) e um `@SqsListener` que sempre lança exceção.
