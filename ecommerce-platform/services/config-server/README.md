# config-server

Configuração centralizada da plataforma. Serve os `application.yml` de todos os outros serviços a partir de um único lugar.

## Papel na arquitetura

Implementação de **Spring Cloud Config Server** (`@EnableConfigServer`), rodando em profile `native` — serve arquivos de um diretório local ([`infrastructure/config-repo/`](../../infrastructure/config-repo/)), não um repositório Git real (ver nota de migração em [`docs/deployment/aws.md`](../../docs/deployment/aws.md)).

Todo outro microsserviço declara `spring.config.import: "optional:configserver:${CONFIG_SERVER_URL:...}"` e busca daqui, na subida, configurações compartilhadas: exposição do Actuator, endpoint OTLP do Jaeger, credenciais/endpoint do LocalStack (SNS/SQS) e o segredo JWT usado por todos os serviços para validar tokens emitidos pelo `auth-service`.

Não tem `domain/`, `application/` nem `infrastructure/` — é a única exceção estrutural do projeto junto com `gateway-service` (ver [`.claude/rules/estrutura-microsservico.md`](../../.claude/rules/estrutura-microsservico.md)), por ser infraestrutura pura, sem regra de negócio própria.

## Endpoints

Nenhum endpoint próprio — usa os endpoints padrão do Spring Cloud Config Server: `GET /{application}/{profile}` retorna a configuração resolvida para aquele serviço/profile. Sem autenticação (uso interno, atrás da rede Docker).

## Configuração

| Propriedade | Valor | Descrição |
|---|---|---|
| `spring.profiles.active` | `native` | Serve config de arquivos locais, não Git |
| `spring.cloud.config.server.native.search-locations` | `file:${CONFIG_REPO_PATH:../../infrastructure/config-repo}` | No Docker, `CONFIG_REPO_PATH=/config-repo` (montado via volume); localmente, caminho relativo ao monorepo |

## Como rodar isoladamente

```bash
docker compose up -d config-server
curl http://localhost:8888/order-service/default
```

Porta: **8888**.

## Exemplo

```bash
# ver a configuracao completa resolvida para o order-service
curl -s http://localhost:8888/order-service/default | python3 -m json.tool
```

## Testes

`ConfigServerApplicationTests.java` — smoke test de subida de contexto (não há regra de negócio própria a testar).
