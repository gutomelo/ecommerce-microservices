# platform-common

Utilitários e tipos genéricos sem regra de negócio: envelope de resposta REST, superclasses JPA e helpers de data/JSON. A dependência mais básica de todo serviço.

## Papel na arquitetura

Base de tudo — nenhum outro módulo `platform-*` depende de nada além deste (e do que ele mesmo trouxer). Não tem `@AutoConfiguration` nem `@ConfigurationProperties`: é só POJOs/records/utilitários estáticos, usados diretamente pelo código de cada serviço.

## API pública

| Tipo | Pacote | Descrição |
|---|---|---|
| `ApiResponse<T>` (record) | `common` | Envelope padrão de **sucesso** para toda resposta REST: `(success, data, message, timestamp)`. Factories: `success(data)`, `success(data, message)`, `successMessage(message)`. |
| `ErrorResponse` (record) | `common` | Envelope padrão de **erro**, construído por `GlobalExceptionHandler` (ver [`platform-exception`](../platform-exception/README.md)): `(timestamp, status, error, message, path, correlationId, violations)`. Factories: `of(status, error, message, path, correlationId)` e uma sobrecarga com `List<FieldViolation>` para erros de validação de campo. |
| `PageResponse<T>` (record) | `common` | Envelope de paginação, desacoplado do `Page<T>` do Spring Data: `(content, page, size, totalElements, totalPages, last)`. Factories: `from(Page<T>)`, `from(Page<S>, Function<S,T> mapper)`. |
| `BaseEntity` | `common` | `@MappedSuperclass` JPA: `id` (`UUID`, `@GeneratedValue`), `equals`/`hashCode` por id. |
| `BaseAuditEntity` | `common` | Estende `BaseEntity`; adiciona `createdAt`/`updatedAt` via `@PrePersist`/`@PreUpdate` — callbacks JPA puros, não exige `@EnableJpaAuditing` em cada serviço. |
| `CorrelationHeaders` | `common.constants` | Constantes de nome de header/chave MDC: `CORRELATION_ID_HEADER`, `TRACE_ID_HEADER`, `CORRELATION_ID_MDC_KEY`, `TRACE_ID_MDC_KEY` — usadas por `platform-observability` e por qualquer serviço que precise ler o `correlationId` do contexto atual. |
| `DateUtils` | `common.util` | Helpers de data/hora em UTC (`nowUtc()`, `formatIsoUtc(Instant)`, `parseIsoUtc(String)`, `isBefore`/`isAfter`). |
| `JsonUtils` | `common.util` | `ObjectMapper` compartilhado (com `JavaTimeModule`) + `toJson(Object)`/`fromJson(String, Class<T>)`; lança `JsonUtils.JsonProcessingRuntimeException` em falha. |

## Como usar

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>platform-common</artifactId>
</dependency>
```

Exemplo típico de controller (padrão usado em todo serviço com API — ver qualquer `*Controller.java` em [`services/`](../../services/)):

```java
@GetMapping("/{id}")
public ApiResponse<ProductResponse> findById(@PathVariable UUID id) {
    return ApiResponse.success(ProductResponse.from(productService.findById(id)));
}
```

## Dependências principais

`jackson-databind` + `jackson-datatype-jsr310`, `spring-data-commons` (só para o tipo `Page<T>` usado por `PageResponse`), `jakarta.persistence-api`.

## Testes

`ApiResponseTest`, `ErrorResponseTest`, `PageResponseTest`, `BaseEntityTest`, `BaseAuditEntityTest`, `util/DateUtilsTest`, `util/JsonUtilsTest` — todos unitários.
