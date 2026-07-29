# platform-observability

Tracing (OpenTelemetry), métricas (Micrometer/Prometheus), health check (Actuator) e log estruturado — configuração idêntica em todo serviço, escrita uma única vez.

## Papel na arquitetura

Duas peças, ambas automáticas por só ter este módulo no classpath:

1. **`CorrelationIdFilter`** (`OncePerRequestFilter`) — garante que toda requisição tenha um `correlationId` (reaproveitado do header `X-Correlation-Id` recebido, ou gerado se ausente) e o `traceId` da *span* OpenTelemetry atual, ambos no MDC para todo log daquela requisição. Registrado com `Ordered.HIGHEST_PRECEDENCE` — precisa rodar antes de qualquer outro filtro (inclusive segurança), senão logs de parte da requisição ficariam sem o `correlationId`.
2. **`logback-platform-base.xml`** — fragmento Logback compartilhado (`net.logstash.logback.encoder.LogstashEncoder`), incluindo `correlationId`/`traceId` do MDC e o nome da aplicação (`spring.application.name`) em todo log JSON. Cada serviço só faz:

   ```xml
   <!-- logback-spring.xml de cada servico -->
   <configuration>
       <include resource="logback-platform-base.xml"/>
   </configuration>
   ```

   Nunca redefine o encoder por conta própria — é assim que todo log da plataforma sai no mesmo formato JSON, agregável em uma stack de observabilidade real.

## Auto-configuração

`ObservabilityAutoConfiguration` (`@ConditionalOnClass(HttpServletRequest.class)`):

| Bean | Condição |
|---|---|
| `CorrelationIdFilter` | `@ConditionalOnMissingBean` |
| `FilterRegistrationBean<CorrelationIdFilter>` (ordem `HIGHEST_PRECEDENCE`) | sempre |

**Por que o guard é `HttpServletRequest` e não `OncePerRequestFilter.class`**: `spring-web` é compartilhado entre Servlet e WebFlux, então `OncePerRequestFilter.class` sempre "existe" no classpath do `gateway-service` (reativo) mesmo sem `jakarta.servlet-api` — só falharia ao ser efetivamente carregada. `HttpServletRequest` só existe quando há de fato um container servlet, então é o guard correto para excluir o `gateway-service` desta autoconfiguração (que implementa seu próprio filtro reativo equivalente).

## Configuração

Nenhum `@ConfigurationProperties` próprio. O endpoint OTLP (Jaeger), amostragem de trace e exposição do Actuator vêm de `config-server` (compartilhados, ver [`infrastructure/config-repo/application.yml`](../../infrastructure/config-repo/application.yml)):

```yaml
management:
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
  tracing:
    sampling:
      probability: 1.0
```

## Como usar

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>platform-observability</artifactId>
</dependency>
```

Nada a chamar explicitamente — o filtro e o log estruturado ficam ativos só por ter o módulo no classpath (mais o `<include>` no `logback-spring.xml`, ver acima).

## Dependências principais

`platform-common` (para `CorrelationHeaders`), `spring-boot-starter-actuator`, `micrometer-registry-prometheus`, `micrometer-tracing` + `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`, `logstash-logback-encoder`.

## Testes

`CorrelationIdFilterTest.java`, `ObservabilityAutoConfigurationTest.java` — unitários.
