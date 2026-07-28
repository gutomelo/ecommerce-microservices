package com.ecommerce.platform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RolesTest {

    @Test
    void authorityHasSpringSecurityPrefix() {
        assertThat(Roles.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
        assertThat(Roles.CUSTOMER.authority()).isEqualTo("ROLE_CUSTOMER");
    }
}
