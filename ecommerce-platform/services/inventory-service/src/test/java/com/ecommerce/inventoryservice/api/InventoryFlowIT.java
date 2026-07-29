package com.ecommerce.inventoryservice.api;

import com.ecommerce.inventoryservice.InventoryServiceApplication;
import com.ecommerce.inventoryservice.infrastructure.outbox.OutboxStatus;
import com.ecommerce.inventoryservice.infrastructure.outbox.SpringDataOutboxRepository;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.events.StockUnavailableEvent;
import com.ecommerce.platform.messaging.MessageSerializer;
import com.ecommerce.platform.testing.PostgresAndLocalStackTestContainerSupport;
import com.ecommerce.platform.testing.TestJwtTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Prova fim a fim de reserva/liberacao de estoque (ver CLAUDE.md): simula
 * OrderCreatedEvent/OrderCancelledEvent chegando na inventory-queue (como
 * order-service faria via SNS fan-out - ver infrastructure/localstack/init/init-aws.sh)
 * e verifica reserva bem-sucedida, indisponibilidade de estoque, compensacao
 * (liberacao) e idempotencia contra LocalStack/Postgres reais.
 */
@SpringBootTest(classes = InventoryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryFlowIT extends PostgresAndLocalStackTestContainerSupport {

    private static final String INVENTORY_QUEUE = "inventory-queue";

    private static final SqsClient SQS_CLIENT = SqsClient.builder()
            .endpointOverride(LOCALSTACK.getEndpointOverride(SQS))
            .region(Region.of(LOCALSTACK.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
            .build();

    private static final String INVENTORY_QUEUE_URL;

    static {
        INVENTORY_QUEUE_URL = SQS_CLIENT.createQueue(b -> b.queueName(INVENTORY_QUEUE)).queueUrl();
    }

    private final TestJwtTokenFactory tokenFactory = new TestJwtTokenFactory();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private MessageSerializer serializer;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenFactory.customerToken());
        return headers;
    }

    @SuppressWarnings("unchecked")
    private int stockLevelOf(UUID productId) {
        var response = restTemplate.exchange("/stock/" + productId, HttpMethod.GET, new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return ((Number) data.get("availableQuantity")).intValue();
    }

    private void sendOrderCreated(UUID orderId, UUID productId, int quantity) {
        SQS_CLIENT.sendMessage(b -> b.queueUrl(INVENTORY_QUEUE_URL)
                .messageBody(orderCreatedJson(orderId, productId, quantity)));
    }

    private String orderCreatedJson(UUID orderId, UUID productId, int quantity) {
        var payload = new OrderCreatedEvent.Payload(orderId, UUID.randomUUID(),
                List.of(new OrderCreatedEvent.Payload.Item(productId, quantity, new BigDecimal("19.90"))),
                new BigDecimal("19.90").multiply(BigDecimal.valueOf(quantity)));
        var event = OrderCreatedEvent.of(orderId.toString(), "corr-it", "trace-it", payload);
        return serializer.serialize(event);
    }

    private long countOutboxRowsForOrder(String eventType, UUID orderId) {
        return outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals(eventType) && e.getAggregateId().equals(orderId.toString()))
                .count();
    }

    @Test
    void reservesStockAndPublishesStockReservedWhenAvailable() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        int initialStock = stockLevelOf(productId);
        String messageBody = orderCreatedJson(orderId, productId, 5);

        SQS_CLIENT.sendMessage(b -> b.queueUrl(INVENTORY_QUEUE_URL).messageBody(messageBody));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var entry = outboxRepository.findAll().stream()
                    .filter(e -> e.getEventType().equals(StockReservedEvent.EVENT_TYPE)
                            && e.getAggregateId().equals(orderId.toString()))
                    .findFirst().orElseThrow();
            assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        });
        assertThat(stockLevelOf(productId)).isEqualTo(initialStock - 5);

        // Reentrega simulando at-least-once delivery do SQS: MESMO eventId (mesma
        // mensagem), a idempotencia (processed_events) deve impedir uma segunda
        // reserva/publicacao - o estoque nao pode ser decrementado duas vezes.
        SQS_CLIENT.sendMessage(b -> b.queueUrl(INVENTORY_QUEUE_URL).messageBody(messageBody));

        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(stockLevelOf(productId)).isEqualTo(initialStock - 5);
            assertThat(countOutboxRowsForOrder(StockReservedEvent.EVENT_TYPE, orderId)).isEqualTo(1);
        });
    }

    @Test
    void publishesStockUnavailableAndReservesNothingWhenQuantityExceedsAvailable() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        int initialStock = stockLevelOf(productId);

        sendOrderCreated(orderId, productId, initialStock + 1);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(countOutboxRowsForOrder(StockUnavailableEvent.EVENT_TYPE, orderId)).isEqualTo(1));
        assertThat(stockLevelOf(productId)).isEqualTo(initialStock);
    }

    @Test
    void releasesStockOnOrderCancelledAfterSuccessfulReservation() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        int initialStock = stockLevelOf(productId);

        sendOrderCreated(orderId, productId, 7);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(stockLevelOf(productId)).isEqualTo(initialStock - 7));

        var cancelPayload = new OrderCancelledEvent.Payload(orderId, UUID.randomUUID(), "pagamento recusado", Instant.now());
        var cancelEvent = OrderCancelledEvent.of(orderId.toString(), "corr-it", "trace-it", cancelPayload);
        SQS_CLIENT.sendMessage(b -> b.queueUrl(INVENTORY_QUEUE_URL).messageBody(serializer.serialize(cancelEvent)));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(stockLevelOf(productId)).isEqualTo(initialStock));
    }
}
