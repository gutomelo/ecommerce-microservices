---
name: arquitetura
description: Princípios arquiteturais obrigatórios — Clean Architecture, DDD, Hexagonal, SOLID, 12-Factor
---

# Arquitetura

- Cada microsserviço segue **Clean Architecture** + **Ports and Adapters (Hexagonal)** com as camadas fixas: `domain/`, `application/`, `infrastructure/`, `api/`, `config/` (ver `.claude/rules/estrutura-microsservico.md`).
- `domain/` não depende de Spring, JPA, SNS/SQS ou qualquer framework de infraestrutura. Depende apenas de `platform-common`/`platform-exception` quando necessário.
- `application/` (casos de uso) orquestra o domínio e define as *ports* (interfaces) que `infrastructure/` implementa. Nunca o inverso.
- Aplicar **DDD tático**: agregados, entidades, value objects, domain events, repositórios como interfaces no domínio/aplicação implementadas na infraestrutura.
- Aplicar **SOLID** e **Clean Code**: sem classes "deus", sem métodos com múltiplas responsabilidades, injeção de dependência via construtor.
- Aplicar **Twelve-Factor App**: configuração via ambiente/Config Server, sem estado em disco local, logs como stream, processos stateless (exceto o próprio banco).
- **Nunca** implementar chamada síncrona (REST/Feign/RestTemplate/WebClient) entre microsserviços para regra de negócio. A única comunicação entre serviços é assíncrona, via eventos (SNS/SQS). Chamadas síncronas só existem entre o Gateway e um serviço, nunca serviço-a-serviço.
- Toda transação distribuída é resolvida via **Saga por Coreografia**: não existe orquestrador central. Cada serviço reage a eventos e publica os próprios eventos.
