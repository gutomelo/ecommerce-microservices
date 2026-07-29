package com.ecommerce.customerservice.api;

import com.ecommerce.customerservice.CustomerServiceApplication;
import com.ecommerce.platform.testing.PostgresTestContainerSupport;
import com.ecommerce.platform.testing.TestJwtTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxo CRUD completo contra um PostgreSQL real (Testcontainers), com JWT real
 * (mesma abordagem de AuthFlowIT em auth-service) - prova que ADMIN pode escrever,
 * CUSTOMER so pode ler, e requisicoes sem token sao rejeitadas.
 */
@SpringBootTest(classes = CustomerServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerCrudFlowIT extends PostgresTestContainerSupport {

    private final TestJwtTokenFactory tokenFactory = new TestJwtTokenFactory();

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminCanCreateReadUpdateAndDeleteCustomer() {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        String adminToken = tokenFactory.adminToken();

        var createResponse = restTemplate.exchange("/customers", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Jane Doe", "email", email, "phone", "123456"), authHeaders(adminToken)),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> created = (Map<String, Object>) createResponse.getBody().get("data");
        String id = (String) created.get("id");
        assertThat(created.get("email")).isEqualTo(email);

        String customerToken = tokenFactory.customerToken();
        var getResponse = restTemplate.exchange("/customers/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(customerToken)), new ParameterizedTypeReference<Map<String, Object>>() {
                });
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> fetched = (Map<String, Object>) getResponse.getBody().get("data");
        assertThat(fetched.get("createdAt")).isNotNull();

        var updateResponse = restTemplate.exchange("/customers/" + id, HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "Jane Smith", "phone", "999999"), authHeaders(adminToken)),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> updated = (Map<String, Object>) updateResponse.getBody().get("data");
        assertThat(updated.get("name")).isEqualTo("Jane Smith");
        // regressao: update ja voltou createdAt nulo no response (a linha do
        // banco ficava correta gracas a updatable=false, mas o adapter
        // reconstruia uma entidade transiente sem createdAt a cada save())
        assertThat(updated.get("createdAt")).isEqualTo(fetched.get("createdAt"));

        var deleteResponse = restTemplate.exchange("/customers/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminToken)), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void customerCannotCreateCustomer() {
        String customerToken = tokenFactory.customerToken();

        var response = restTemplate.exchange("/customers", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "X", "email", "x@example.com"), authHeaders(customerToken)),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requestWithoutTokenIsRejected() {
        // Spring Security trata requisicao sem credenciais como "anonimo autenticado";
        // negar acesso por falta de role resulta em 403, nao 401 (comportamento padrao).
        var response = restTemplate.getForEntity("/customers/" + UUID.randomUUID(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void creatingDuplicateEmailReturnsConflict() {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        String adminToken = tokenFactory.adminToken();
        var body = Map.of("name", "Jane Doe", "email", email);

        restTemplate.exchange("/customers", HttpMethod.POST, new HttpEntity<>(body, authHeaders(adminToken)), Void.class);
        var secondResponse = restTemplate.exchange("/customers", HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(adminToken)), Void.class);

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
