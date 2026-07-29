package com.ecommerce.platform.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de JWT compartilhadas por toda a plataforma. Cada servico define
 * apenas o valor de {@code platform.security.jwt.secret} (via Config Server); o
 * restante ja vem com defaults sensatos.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "platform.security.jwt")
public class JwtProperties {

    /**
     * Segredo HMAC usado para assinar/validar tokens. Deve ter pelo menos 32
     * caracteres (256 bits) para HS256. Nunca deve ser commitado em texto plano
     * em ambiente real - apenas em configuracao local via LocalStack/Docker Compose.
     */
    private String secret;

    private String issuer = "ecommerce-platform";

    private long accessTokenExpirationMinutes = 15;

    private long refreshTokenExpirationDays = 7;
}
