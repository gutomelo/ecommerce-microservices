package com.ecommerce.authservice.application;

import com.ecommerce.authservice.application.port.PasswordHasher;
import com.ecommerce.authservice.domain.User;
import com.ecommerce.authservice.domain.port.UserRepository;
import com.ecommerce.platform.exception.UnauthorizedException;
import com.ecommerce.platform.security.JwtProperties;
import com.ecommerce.platform.security.JwtTokenProvider;
import com.ecommerce.platform.security.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters!!";

    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private JwtTokenProvider jwtTokenProvider;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);

        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        jwtTokenProvider = new JwtTokenProvider(properties);

        service = new AuthenticationService(userRepository, passwordHasher, jwtTokenProvider);
    }

    private User activeUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("jane@example.com")
                .passwordHash("hashed-password")
                .role(Roles.CUSTOMER)
                .active(true)
                .build();
    }

    @Test
    void loginIssuesTokenPairForValidCredentials() {
        User user = activeUser();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("plain-password", "hashed-password")).thenReturn(true);

        var tokens = service.login("jane@example.com", "plain-password");

        assertThat(jwtTokenProvider.isValid(tokens.accessToken())).isTrue();
        assertThat(jwtTokenProvider.isValid(tokens.refreshToken())).isTrue();
        assertThat(jwtTokenProvider.getSubject(tokens.accessToken())).isEqualTo("jane@example.com");
        assertThat(jwtTokenProvider.getRole(tokens.accessToken())).isEqualTo(Roles.CUSTOMER);
    }

    @Test
    void loginFailsForUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("unknown@example.com", "any-password"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginFailsForWrongPassword() {
        User user = activeUser();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> service.login("jane@example.com", "wrong-password"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginFailsForInactiveUser() {
        User inactiveUser = activeUser().toBuilder().active(false).build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> service.login("jane@example.com", "plain-password"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshIssuesNewTokenPairForValidRefreshToken() {
        User user = activeUser();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole());
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        var tokens = service.refresh(refreshToken);

        assertThat(jwtTokenProvider.isValid(tokens.accessToken())).isTrue();
        assertThat(jwtTokenProvider.getSubject(tokens.accessToken())).isEqualTo("jane@example.com");
    }

    @Test
    void refreshFailsForInvalidToken() {
        assertThatThrownBy(() -> service.refresh("not-a-valid-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshFailsWhenUserNoLongerExists() {
        String refreshToken = jwtTokenProvider.generateRefreshToken("ghost@example.com", Roles.CUSTOMER);
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(refreshToken))
                .isInstanceOf(UnauthorizedException.class);
    }
}
