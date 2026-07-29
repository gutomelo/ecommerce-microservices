package com.ecommerce.productservice.api;

import com.ecommerce.platform.testing.PostgresTestContainerSupport;
import com.ecommerce.platform.testing.TestJwtTokenFactory;
import com.ecommerce.productservice.ProductServiceApplication;
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
 * Fluxo CRUD completo contra um PostgreSQL real (Testcontainers), com JWT real -
 * mesma abordagem de CustomerCrudFlowIT.
 */
@SpringBootTest(classes = ProductServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductCrudFlowIT extends PostgresTestContainerSupport {

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
    void adminCanCreateReadUpdateAndDeleteProduct() {
        String adminToken = tokenFactory.adminToken();

        var createResponse = restTemplate.exchange("/products", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Widget", "description", "A widget", "category", "tools",
                        "price", 19.90, "stock", 10), authHeaders(adminToken)),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> created = (Map<String, Object>) createResponse.getBody().get("data");
        String id = (String) created.get("id");
        assertThat(created.get("name")).isEqualTo("Widget");

        String customerToken = tokenFactory.customerToken();
        var getResponse = restTemplate.exchange("/products/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders(customerToken)), new ParameterizedTypeReference<Map<String, Object>>() {
                });
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> fetched = (Map<String, Object>) getResponse.getBody().get("data");
        assertThat(fetched.get("createdAt")).isNotNull();

        var updateResponse = restTemplate.exchange("/products/" + id, HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "Gadget", "description", "desc", "category", "electronics",
                        "price", 29.90, "stock", 5), authHeaders(adminToken)),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> updated = (Map<String, Object>) updateResponse.getBody().get("data");
        assertThat(updated.get("name")).isEqualTo("Gadget");
        assertThat(((Number) updated.get("stock")).intValue()).isEqualTo(5);
        // regressao: update ja voltou createdAt nulo no response (a linha do
        // banco ficava correta gracas a updatable=false, mas o adapter
        // reconstruia uma entidade transiente sem createdAt a cada save())
        assertThat(updated.get("createdAt")).isEqualTo(fetched.get("createdAt"));

        var deleteResponse = restTemplate.exchange("/products/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminToken)), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void customerCannotCreateProduct() {
        String customerToken = tokenFactory.customerToken();

        var response = restTemplate.exchange("/products", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Widget", "category", "tools", "price", 19.90, "stock", 10),
                        authHeaders(customerToken)),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void customerCanReadProducts() {
        String customerToken = tokenFactory.customerToken();

        var response = restTemplate.exchange("/products", HttpMethod.GET,
                new HttpEntity<>(authHeaders(customerToken)), new ParameterizedTypeReference<Map<String, Object>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void requestWithoutTokenIsRejected() {
        var response = restTemplate.getForEntity("/products/" + UUID.randomUUID(), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void invalidPriceIsRejectedByValidation() {
        String adminToken = tokenFactory.adminToken();

        var response = restTemplate.exchange("/products", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "Widget", "category", "tools", "price", -5.0, "stock", 10),
                        authHeaders(adminToken)),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
