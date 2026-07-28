package com.ecommerce.authservice.application;

import com.ecommerce.authservice.application.port.PasswordHasher;
import com.ecommerce.authservice.domain.User;
import com.ecommerce.authservice.domain.port.UserRepository;
import com.ecommerce.platform.exception.ConflictException;
import org.springframework.stereotype.Service;

@Service
public class RegisterUserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ja existe uma conta cadastrada com este e-mail");
        }

        User user = User.register(email, passwordHasher.hash(rawPassword));
        return userRepository.save(user);
    }
}
