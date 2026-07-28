package com.ecommerce.platform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Helpers de autenticacao/autorizacao para uso dentro dos casos de uso dos
 * microsservicos, evitando acesso disperso e inconsistente ao SecurityContext.
 */
public final class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    public static Optional<String> currentSubject() {
        return currentAuthentication().map(Authentication::getName);
    }

    public static boolean hasRole(Roles role) {
        return currentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals(role.authority())))
                .orElse(false);
    }

    private static Optional<Authentication> currentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }
}
