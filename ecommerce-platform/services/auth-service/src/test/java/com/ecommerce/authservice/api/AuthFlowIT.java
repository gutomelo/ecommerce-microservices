package com.ecommerce.authservice.api;

import com.ecommerce.authservice.AuthServiceApplication;
import com.ecommerce.platform.testing.PostgresTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra um PostgreSQL real (Testcontainers), o fluxo completo do
 * auth-service: registrar -> logar -> renovar o token - exatamente o que o
 * Marco 7 do plano de implementacao pede como verificacao.
 */
@SpringBootTest(classes = AuthServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIT extends PostgresTestContainerSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void registerLoginAndRefreshFlow() {
        String email = "flow-" + UUID.randomUUID() + "@example.com";
        String password = "Sup3rSecret!";

        var registerResponse = post("/auth/register", Map.of("email", email, "password", password));
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> registerData = (Map<String, Object>) registerResponse.getBody().get("data");
        assertThat(registerData.get("email")).isEqualTo(email);
        assertThat(registerData.get("role")).isEqualTo("CUSTOMER");

        var loginResponse = post("/auth/login", Map.of("email", email, "password", password));
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> loginData = (Map<String, Object>) loginResponse.getBody().get("data");
        String accessToken = (String) loginData.get("accessToken");
        String refreshToken = (String) loginData.get("refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(loginData.get("tokenType")).isEqualTo("Bearer");

        var refreshResponse = post("/auth/refresh", Map.of("refreshToken", refreshToken));
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> refreshData = (Map<String, Object>) refreshResponse.getBody().get("data");
        assertThat((String) refreshData.get("accessToken")).isNotBlank();
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() {
        String email = "wrongpass-" + UUID.randomUUID() + "@example.com";
        post("/auth/register", Map.of("email", email, "password", "Correct123!"));

        var response = post("/auth/login", Map.of("email", email, "password", "wrong-password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void registeringSameEmailTwiceReturnsConflict() {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        post("/auth/register", Map.of("email", email, "password", "Correct123!"));

        var response = post("/auth/register", Map.of("email", email, "password", "Correct123!"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void seededAdminCanLogin() {
        var response = post("/auth/login",
                Map.of("email", "admin@ecommerce-platform.local", "password", "Admin@12345"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private org.springframework.http.ResponseEntity<Map<String, Object>> post(String path, Object body) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }
}
