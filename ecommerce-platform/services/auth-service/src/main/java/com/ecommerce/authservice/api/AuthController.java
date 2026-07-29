package com.ecommerce.authservice.api;

import com.ecommerce.authservice.api.dto.AuthResponse;
import com.ecommerce.authservice.api.dto.LoginRequest;
import com.ecommerce.authservice.api.dto.RefreshRequest;
import com.ecommerce.authservice.api.dto.RegisterRequest;
import com.ecommerce.authservice.api.dto.UserResponse;
import com.ecommerce.authservice.application.AuthenticationService;
import com.ecommerce.authservice.application.RegisterUserService;
import com.ecommerce.platform.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUserService registerUserService;
    private final AuthenticationService authenticationService;

    public AuthController(RegisterUserService registerUserService, AuthenticationService authenticationService) {
        this.registerUserService = registerUserService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var user = registerUserService.register(request.email(), request.password());
        return ApiResponse.success(UserResponse.from(user), "Usuario cadastrado com sucesso");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        var tokens = authenticationService.login(request.email(), request.password());
        return ApiResponse.success(AuthResponse.of(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var tokens = authenticationService.refresh(request.refreshToken());
        return ApiResponse.success(AuthResponse.of(tokens.accessToken(), tokens.refreshToken()));
    }
}
