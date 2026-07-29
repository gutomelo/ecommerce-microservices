# infrastructure/config-repo

Configuração centralizada servida pelo `config-server` (profile `native`). Todo microsserviço busca daqui na subida, antes de ler seu próprio `application.yml`.

## O que faz

`config-server` monta este diretório (via `spring.cloud.config.server.native.search-locations`) e serve `GET /{application}/{profile}` para qualquer serviço que declare `spring.config.import: "optional:configserver:..."` — que é todo serviço do projeto. Hoje há um único arquivo, [`application.yml`](application.yml), aplicado a **todos** eles (Spring Cloud Config sempre serve o `application.yml` genérico combinado com o `<nome-do-servico>.yml` específico, se existir — este projeto ainda não precisou de nenhum override por serviço).

## O que está aqui

| Chave | Valor | Por quê fica centralizado |
|---|---|---|
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` | Todo serviço expõe o mesmo conjunto de endpoints do Actuator |
| `management.otlp.tracing.endpoint` | `http://jaeger:4318/v1/traces` | Todo serviço exporta trace para o mesmo Jaeger |
| `management.tracing.sampling.probability` | `1.0` | 100% de amostragem — ambiente de demonstração, não produção real |
| `spring.cloud.aws.region.static` / `credentials.*` / `endpoint` | `us-east-1` / `test`/`test` / `http://localstack:4566` | Todo serviço aponta para o **mesmo** LocalStack |
| `platform.security.jwt.secret` / `issuer` / `*-expiration-*` | (ver arquivo) | **O mais crítico**: `auth-service` emite o JWT e todo outro serviço valida — precisam do mesmo segredo. Fica aqui e não em cada serviço individualmente para nunca divergir. |

## Por que "native" e não Git

Spring Cloud Config Server normalmente serve configuração de um repositório Git real. Este projeto usa o profile `native` (arquivos locais) de propósito, para `docker compose up` funcionar sem depender de um repositório Git externo alcançável — ver nota de migração em [`docs/deployment/aws.md`](../../docs/deployment/aws.md) (trocar por Git real, ou por AWS Parameter Store/AppConfig).

## Como inspecionar

```bash
curl -s http://localhost:8888/order-service/default | python3 -m json.tool
```

## Referências

- [`services/config-server/README.md`](../../services/config-server/README.md) — o serviço que serve este diretório.
- [`platform/platform-security/README.md`](../../platform/platform-security/README.md) — quem consome `platform.security.jwt.*`.
