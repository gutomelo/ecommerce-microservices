# auth-service

Login, cadastro e emissão/renovação de JWT. Todo outro serviço confia nos tokens que este serviço emite.

## Papel na arquitetura

Único serviço responsável por autenticação. Não participa da Saga (não consome nem publica eventos). Emite tokens JWT usando `JwtTokenProvider` de `platform-security`, com o mesmo segredo (`platform.security.jwt.secret`) compartilhado por todos os serviços via `config-server` — cada serviço valida o token de novo por conta própria (defesa em profundidade), nunca confiando cegamente no header repassado pelo `gateway-service`.

Todo usuário criado por `POST /auth/register` recebe a role `CUSTOMER`. Não existe endpoint para criar um `ADMIN` — o único usuário administrador vem semeado via migration Flyway (ver abaixo).

## Endpoints

| Método | Path | Papel exigido | Descrição |
|---|---|---|---|
| `POST` | `/auth/register` | público | Cria usuário (sempre role `CUSTOMER`) |
| `POST` | `/auth/login` | público | Autentica, retorna par de tokens |
| `POST` | `/auth/refresh` | público | Troca um refresh token válido por um novo par |

## Modelo de dados

| Tabela | Descrição |
|---|---|
| `users` | `id`, `email` (único), `password_hash` (BCrypt), `role`, `active`, `created_at`, `updated_at` |

A migration `V1__create_users_table.sql` semeia um usuário `ADMIN`:

- e-mail: `admin@ecommerce-platform.local`
- senha: `Admin@12345`

(hash BCrypt gerado uma vez e versionado na migration — senha em texto puro existe só para fins de demonstração deste projeto de portfólio, nunca faça isso em produção real).

## Configuração

| Propriedade | Default local |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5433` / `auth_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `auth` / `auth` |

Segredo JWT, emissor e tempos de expiração vêm do `config-server` (`platform.security.jwt.*`, compartilhado por todos os serviços).

## Como rodar isoladamente

```bash
docker compose up -d postgres-auth config-server auth-service
```

Porta: **8081**. Swagger UI: `http://localhost:8081/swagger-ui.html`.

## Exemplos (curl)

```bash
# registrar um novo cliente
curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente@example.com","password":"SenhaForte123"}'

# login
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ecommerce-platform.local","password":"Admin@12345"}'
# -> {"success":true,"data":{"accessToken":"...","refreshToken":"...","tokenType":"Bearer"}}

# renovar o access token usando o refresh token
curl -s -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh-token-recebido-no-login>"}'
```

> Exemplos acima passam pelo `gateway-service` (porta 8080, ver [`services/gateway-service/README.md`](../gateway-service/README.md)); para chamar o `auth-service` diretamente, use a porta 8081.

## Testes

- `AuthFlowIT.java` — fluxo completo registro → login → refresh, contra um PostgreSQL real (Testcontainers).
- `AuthenticationServiceTest.java`, `RegisterUserServiceTest.java` — casos de uso, com mocks.
- `UserTest.java` — regras do agregado de domínio.
