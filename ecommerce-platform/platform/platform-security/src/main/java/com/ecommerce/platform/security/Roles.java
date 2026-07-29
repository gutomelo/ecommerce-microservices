package com.ecommerce.platform.security;

/**
 * Roles suportadas pela plataforma (ver .claude/rules/seguranca.md). Toda regra
 * de autorizacao deve ser expressa em termos destas roles.
 */
public enum Roles {

    ADMIN,
    CUSTOMER;

    private static final String SPRING_SECURITY_PREFIX = "ROLE_";

    /**
     * Nome da authority no formato esperado pelo Spring Security (ex.: "ROLE_ADMIN").
     */
    public String authority() {
        return SPRING_SECURITY_PREFIX + name();
    }
}
