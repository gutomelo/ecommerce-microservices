package com.ecommerce.platform.testing;

import com.ecommerce.platform.security.JwtProperties;
import com.ecommerce.platform.security.JwtTokenProvider;
import com.ecommerce.platform.security.Roles;

/**
 * Gera tokens JWT validos para testes de integracao de endpoints protegidos, sem
 * repetir a configuracao de {@link JwtTokenProvider} em cada microsservico. O
 * servico sob teste precisa configurar {@code platform.security.jwt.secret} com o
 * mesmo valor de {@link #TEST_SECRET} (normalmente via application.yml de teste).
 */
public class TestJwtTokenFactory {

    public static final String TEST_SECRET = "test-secret-key-with-at-least-32-characters!!";

    private final JwtTokenProvider tokenProvider;

    public TestJwtTokenFactory() {
        this(TEST_SECRET);
    }

    public TestJwtTokenFactory(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        this.tokenProvider = new JwtTokenProvider(properties);
    }

    public String adminToken() {
        return adminToken("admin@example.com");
    }

    public String adminToken(String subject) {
        return tokenProvider.generateAccessToken(subject, Roles.ADMIN);
    }

    public String customerToken() {
        return customerToken("customer@example.com");
    }

    public String customerToken(String subject) {
        return tokenProvider.generateAccessToken(subject, Roles.CUSTOMER);
    }

    public JwtTokenProvider tokenProvider() {
        return tokenProvider;
    }
}
