package com.ecommerce.authservice.application;

import com.ecommerce.authservice.application.port.PasswordHasher;
import com.ecommerce.authservice.domain.User;
import com.ecommerce.authservice.domain.port.UserRepository;
import com.ecommerce.platform.exception.UnauthorizedException;
import com.ecommerce.platform.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "E-mail ou senha invalidos";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthenticationService(UserRepository userRepository, PasswordHasher passwordHasher,
                                  JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public TokenPair login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE);
        }

        return issueTokens(user);
    }

    public TokenPair refresh(String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw new UnauthorizedException("Refresh token invalido ou expirado");
        }

        String email = jwtTokenProvider.getSubject(refreshToken);
        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Usuario nao encontrado ou inativo"));

        return issueTokens(user);
    }

    private TokenPair issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole());
        return new TokenPair(accessToken, refreshToken);
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }
}
