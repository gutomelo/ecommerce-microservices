---
name: seguranca
description: Autenticação JWT via platform-security, roles e proteção de endpoints
---

# Segurança

- Autenticação/autorização via **JWT**, emitido e validado pelo `auth-service`; demais serviços validam o token reutilizando os componentes de `platform-security` (`JwtTokenProvider`, `JwtAuthenticationFilter`) — nunca reimplementar validação de JWT em um serviço específico.
- Roles suportadas: `ADMIN` e `CUSTOMER`. Toda regra de autorização deve ser expressa em termos dessas roles (ou permissões derivadas definidas em `platform-security`).
- Todos os endpoints são protegidos por padrão. Endpoints públicos (ex.: login, cadastro, refresh token no `auth-service`) precisam de exceção explícita e justificada na configuração de segurança do serviço.
- O `gateway-service` é responsável pela validação inicial do JWT e roteamento; os serviços internos ainda validam o token novamente (defesa em profundidade), nunca confiam cegamente em um cabeçalho repassado pelo gateway sem validação própria.
