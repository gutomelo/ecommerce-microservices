package com.ecommerce.platform.testing;

import com.ecommerce.platform.security.Roles;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestJwtTokenFactoryTest {

    private final TestJwtTokenFactory factory = new TestJwtTokenFactory();

    @Test
    void adminTokenHasAdminRole() {
        String token = factory.adminToken();

        assertThat(factory.tokenProvider().isValid(token)).isTrue();
        assertThat(factory.tokenProvider().getRole(token)).isEqualTo(Roles.ADMIN);
        assertThat(factory.tokenProvider().getSubject(token)).isEqualTo("admin@example.com");
    }

    @Test
    void customerTokenHasCustomerRoleAndCustomSubject() {
        String token = factory.customerToken("jane@example.com");

        assertThat(factory.tokenProvider().isValid(token)).isTrue();
        assertThat(factory.tokenProvider().getRole(token)).isEqualTo(Roles.CUSTOMER);
        assertThat(factory.tokenProvider().getSubject(token)).isEqualTo("jane@example.com");
    }

    @Test
    void factoryWithCustomSecretStillProducesValidTokens() {
        TestJwtTokenFactory customFactory = new TestJwtTokenFactory("another-test-secret-with-32-characters!!");

        String token = customFactory.adminToken();

        assertThat(customFactory.tokenProvider().isValid(token)).isTrue();
        assertThat(factory.tokenProvider().isValid(token)).isFalse();
    }
}
