package com.ecommerce.platform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters!!";

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("ecommerce-platform-test");
        properties.setAccessTokenExpirationMinutes(15);
        properties.setRefreshTokenExpirationDays(7);
        tokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void generatesAndValidatesAccessToken() {
        String token = tokenProvider.generateAccessToken("user@example.com", Roles.ADMIN);

        assertThat(tokenProvider.isValid(token)).isTrue();
        assertThat(tokenProvider.getSubject(token)).isEqualTo("user@example.com");
        assertThat(tokenProvider.getRole(token)).isEqualTo(Roles.ADMIN);
    }

    @Test
    void generatesAndValidatesRefreshToken() {
        String token = tokenProvider.generateRefreshToken("customer@example.com", Roles.CUSTOMER);

        assertThat(tokenProvider.isValid(token)).isTrue();
        assertThat(tokenProvider.getSubject(token)).isEqualTo("customer@example.com");
        assertThat(tokenProvider.getRole(token)).isEqualTo(Roles.CUSTOMER);
    }

    @Test
    void invalidTokenIsNotValid() {
        assertThat(tokenProvider.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void tokenSignedWithDifferentSecretIsNotValid() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("another-secret-key-with-32-characters!!");
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProperties);

        String token = otherProvider.generateAccessToken("user@example.com", Roles.ADMIN);

        assertThat(tokenProvider.isValid(token)).isFalse();
    }

    @Test
    void expiredTokenIsNotValid() throws InterruptedException {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpirationMinutes(0);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(properties);

        String token = shortLivedProvider.generateAccessToken("user@example.com", Roles.CUSTOMER);
        Thread.sleep(50);

        assertThat(shortLivedProvider.isValid(token)).isFalse();
    }
}
