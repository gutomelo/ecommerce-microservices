# platform-exception

Exceções de negócio compartilhadas e um `@RestControllerAdvice` global — cada serviço ganha tratamento de erro consistente só por ter este módulo no classpath, sem escrever nada.

## Papel na arquitetura

`BusinessException` é abstrata com construtores `protected` — nunca é lançada diretamente, só suas subclasses. `GlobalExceptionHandler` mapeia cada uma para um HTTP status e devolve sempre um `ErrorResponse` (de [`platform-common`](../platform-common/README.md)), lendo o `correlationId` atual do MDC.

## API pública

| Exceção | `errorCode` | HTTP status |
|---|---|---|
| `ResourceNotFoundException` | `RESOURCE_NOT_FOUND` | 404 |
| `ConflictException` | `CONFLICT` | 409 |
| `ValidationException` | `VALIDATION_ERROR` | 400 |
| `UnauthorizedException` | `UNAUTHORIZED` | 401 |
| `ForbiddenException` | `FORBIDDEN` | 403 |
| `IntegrationException` | `INTEGRATION_ERROR` | 502 (logado como warn — falha técnica chamando SNS/SQS/outro sistema, tipicamente retry-elegível) |
| `BusinessException` (genérica, via subclasse não listada) | — | 422 |
| `MethodArgumentNotValidException` (bean validation, não é deste módulo) | — | 400, com lista de `FieldViolation` por campo |
| `Exception` (catch-all) | — | 500 (logado como error) |

`ResourceNotFoundException` tem uma factory conveniente: `forId(String resourceName, Object id)` → mensagem `"<resourceName> nao encontrado(a) para o id: <id>"`.

## Auto-configuração

`ExceptionAutoConfiguration` (`@AutoConfiguration @ConditionalOnClass(HttpServletRequest.class)`) registra `GlobalExceptionHandler` como bean automaticamente — **sem isso, ele nunca vira bean**: `@RestControllerAdvice` sozinho não é suficiente porque o pacote `com.ecommerce.platform.exception` fica fora do base package de component-scan de cada serviço (`com.ecommerce.<servico>`). Guardado por `HttpServletRequest` pelo mesmo motivo de `platform-observability`/`platform-security`: não se aplica ao `gateway-service` (WebFlux).

Registrado via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, ativado automaticamente por qualquer serviço que tenha este módulo no classpath — nada a configurar.

## Como usar

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>platform-exception</artifactId>
</dependency>
```

```java
public Product findById(UUID id) {
    return productRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.forId("Product", id));
}
```

## Dependências principais

`platform-common`, `spring-web`, `spring-boot-autoconfigure`, `jakarta.validation-api`, `jakarta.servlet-api` (provided).

## Testes

`BusinessExceptionsTest.java`, `ExceptionAutoConfigurationTest.java`, `GlobalExceptionHandlerTest.java` — todos unitários.
