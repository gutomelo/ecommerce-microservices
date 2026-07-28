---
name: modulo-platform
description: O que pode e o que não pode existir no módulo compartilhado platform/
---

# Módulo Platform

O `platform/` **não é um microsserviço**: não tem banco de dados, não expõe API REST, não roda como aplicação independente. É biblioteca Maven consumida pelos serviços.

Submódulos e responsabilidade única de cada um:

- `platform-common`: `ApiResponse`, `ErrorResponse`, `PageResponse`, `BaseEntity`, `BaseAuditEntity`, utilitários de data/JSON, conversores, validações e constantes genéricas.
- `platform-events`: contratos de evento (`BaseEvent` e todos os eventos de domínio). Único lugar onde eventos são definidos.
- `platform-security`: JWT utilities, `JwtAuthenticationFilter`, `JwtTokenProvider`, roles, permissions, constantes de segurança.
- `platform-messaging`: abstrações de publicação/consumo (`EventPublisher`, `EventConsumer`, `MessageSerializer`, `MessageDeserializer`), configuração de SNS/SQS/LocalStack, retry e tratamento de erro de mensageria.
- `platform-observability`: configuração de OpenTelemetry, Micrometer, Actuator, correlation ID/trace ID, MDC, logging estruturado — idêntica para todos os serviços.
- `platform-exception`: `BusinessException`, `ValidationException`, `ResourceNotFoundException`, `ConflictException`, `UnauthorizedException`, `ForbiddenException`, `IntegrationException`, e um exception handler padrão.
- `platform-testing`: builders, factories, fakes, dados mock, utilitários de Testcontainers e configuração de teste compartilhada.
- `platform-bom`: BOM Maven centralizando versões (Spring Boot, Spring Cloud, Spring Cloud AWS, driver PostgreSQL, Flyway, OpenAPI, Lombok, MapStruct, JUnit, Mockito, Testcontainers, Micrometer, OpenTelemetry, Resilience4j).

## Proibido em `platform/`

Regra de negócio, casos de uso, entidades de domínio, repositórios, controllers, services específicos de um microsserviço, banco de dados próprio, configuração exclusiva de um único serviço.

## Regra de versão

Nenhum microsserviço declara versão própria de uma dependência já centralizada em `platform-bom`. Todo `pom.xml` de serviço importa o BOM.
