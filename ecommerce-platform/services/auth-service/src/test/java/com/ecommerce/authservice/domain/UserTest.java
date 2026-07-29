package com.ecommerce.authservice.domain;

import com.ecommerce.platform.security.Roles;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void registerCreatesActiveCustomer() {
        User user = User.register("jane@example.com", "hashed-password");

        assertThat(user.getEmail()).isEqualTo("jane@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getRole()).isEqualTo(Roles.CUSTOMER);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getId()).isNull();
    }

    @Test
    void equalsAndHashCodeAreBasedOnId() {
        User a = User.builder().id(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111")).build();
        User b = User.builder().id(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111")).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
