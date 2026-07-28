package com.ecommerce.authservice.application;

import com.ecommerce.authservice.application.port.PasswordHasher;
import com.ecommerce.authservice.domain.User;
import com.ecommerce.authservice.domain.port.UserRepository;
import com.ecommerce.platform.exception.ConflictException;
import com.ecommerce.platform.security.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterUserServiceTest {

    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        service = new RegisterUserService(userRepository, passwordHasher);
    }

    @Test
    void registersNewUserWithHashedPassword() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordHasher.hash("plain-password")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.register("jane@example.com", "plain-password");

        assertThat(result.getEmail()).isEqualTo("jane@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(result.getRole()).isEqualTo(Roles.CUSTOMER);
    }

    @Test
    void throwsConflictWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("jane@example.com", "plain-password"))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }
}
