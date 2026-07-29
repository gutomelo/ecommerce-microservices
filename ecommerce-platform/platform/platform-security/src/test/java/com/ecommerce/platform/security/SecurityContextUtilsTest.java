package com.ecommerce.platform.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentSubjectIsEmptyWhenNotAuthenticated() {
        assertThat(SecurityContextUtils.currentSubject()).isEmpty();
        assertThat(SecurityContextUtils.hasRole(Roles.ADMIN)).isFalse();
    }

    @Test
    void currentSubjectAndRoleReflectAuthentication() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "customer@example.com", null, List.of(new SimpleGrantedAuthority(Roles.CUSTOMER.authority())));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityContextUtils.currentSubject()).contains("customer@example.com");
        assertThat(SecurityContextUtils.hasRole(Roles.CUSTOMER)).isTrue();
        assertThat(SecurityContextUtils.hasRole(Roles.ADMIN)).isFalse();
    }
}
