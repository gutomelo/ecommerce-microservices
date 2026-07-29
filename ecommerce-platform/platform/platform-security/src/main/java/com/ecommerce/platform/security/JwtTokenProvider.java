package com.ecommerce.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Geracao e validacao de tokens JWT (access e refresh). Unica implementacao usada
 * por toda a plataforma (ver .claude/rules/seguranca.md) - nenhum servico deve
 * reimplementar geracao/validacao de token.
 */
public class JwtTokenProvider {

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.getIssuer();
        this.accessTokenTtl = Duration.ofMinutes(properties.getAccessTokenExpirationMinutes());
        this.refreshTokenTtl = Duration.ofDays(properties.getRefreshTokenExpirationDays());
    }

    public String generateAccessToken(String subject, Roles role) {
        return buildToken(subject, role, accessTokenTtl);
    }

    public String generateRefreshToken(String subject, Roles role) {
        return buildToken(subject, role, refreshTokenTtl);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Roles getRole(String token) {
        String role = parseClaims(token).get(SecurityConstants.ROLE_CLAIM, String.class);
        return Roles.valueOf(role);
    }

    private String buildToken(String subject, Roles role, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim(SecurityConstants.ROLE_CLAIM, role.name())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
