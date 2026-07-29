# platform-security

JWT — geração, validação, roles. `auth-service` emite tokens usando exatamente as mesmas classes que todo outro serviço usa para validá-los.

## Papel na arquitetura

`JwtTokenProvider` é a única implementação de emissão/validação de JWT da plataforma (JJWT por baixo dos panos) — nenhum serviço reimplementa isso. Todo serviço protegido valida o token de novo por conta própria (defesa em profundidade, ver [`.claude/rules/seguranca.md`](../../.claude/rules/seguranca.md)), mesmo depois do `gateway-service` já ter validado na borda.

## API pública

| Tipo | Descrição |
|---|---|
| `Roles` (enum) | `ADMIN`, `CUSTOMER`. `authority()` → `"ROLE_" + name()`, para o Spring Security. |
| `JwtProperties` | `@ConfigurationProperties(prefix = "platform.security.jwt")` — ver abaixo. |
| `JwtTokenProvider` | `generateAccessToken(subject, role)`, `generateRefreshToken(subject, role)`, `isValid(token)`, `parseClaims(token)`, `getSubject(token)`, `getRole(token)`. Puro Java — sem dependência de Servlet, por isso funciona também no `gateway-service` (WebFlux). |
| `JwtAuthenticationFilter` (`OncePerRequestFilter`) | Extrai e valida o Bearer token de cada requisição, populando o `SecurityContextHolder`. A regra de qual role pode acessar qual endpoint continua em cada serviço (`SecurityConfig` próprio). |
| `SecurityContextUtils` | `currentSubject()` → `Optional<String>`, `hasRole(Roles)` → `boolean` — evita espalhar acesso direto a `SecurityContextHolder` pelo código de caso de uso. |
| `SecurityConstants` | `AUTHORIZATION_HEADER`, `BEARER_PREFIX`, `ROLE_CLAIM`. |

## Auto-configuração

Dividida em **duas** classes — não por acaso:

- **`JwtAutoConfiguration`** (sem `@ConditionalOnClass`): registra `JwtTokenProvider`. Sem dependência de Servlet, seguro para qualquer serviço, incluindo `gateway-service`.
- **`JwtServletAutoConfiguration`** (`@ConditionalOnClass(HttpServletRequest.class)`, `@AutoConfigureAfter(JwtAutoConfiguration.class)`): registra `JwtAuthenticationFilter`.

**Por que duas classes e não uma com `@ConditionalOnClass` no método**: `Class.getDeclaredMethods()` falha ao introspectar a classe inteira se **qualquer** método referenciar um tipo Servlet ausente do classpath — no `gateway-service` (WebFlux puro, sem `jakarta.servlet-api`), isso derrubava a aplicação inteira mesmo com o método individual guardado. O guard precisa estar na **classe**, não no método. Mesmo padrão aplicado em [`platform-exception`](../platform-exception/README.md) e [`platform-observability`](../platform-observability/README.md).

## Configuração

`JwtProperties`, prefixo `platform.security.jwt` (valores compartilhados via `config-server`, ver [`infrastructure/config-repo/application.yml`](../../infrastructure/config-repo/application.yml)):

| Propriedade | Default | Descrição |
|---|---|---|
| `secret` | — (obrigatório) | Deve ter ≥ 32 caracteres (256 bits) para HS256; nunca versionar em texto puro em ambiente real |
| `issuer` | `ecommerce-platform` | |
| `access-token-expiration-minutes` | `15` | |
| `refresh-token-expiration-days` | `7` | |

## Como usar

```xml
<dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>platform-security</artifactId>
</dependency>
```

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/products/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/products/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

Para gerar tokens em teste sem subir um `auth-service` de verdade, use `TestJwtTokenFactory` de [`platform-testing`](../platform-testing/README.md).

## Dependências principais

`jjwt-api` (compile) + `jjwt-impl`/`jjwt-jackson` (runtime), `spring-security-core`, `spring-web`, `spring-boot-autoconfigure`.

## Testes

`JwtTokenProviderTest.java`, `JwtAuthenticationFilterTest.java`, `JwtAutoConfigurationTest.java`, `RolesTest.java`, `SecurityContextUtilsTest.java` — todos unitários.
