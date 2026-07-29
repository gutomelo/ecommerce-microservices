package com.ecommerce.authservice.application.port;

/**
 * Porta de hashing de senha - a aplicacao nao depende diretamente do Spring Security.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
