package com.ecommerce.orderservice.api;

import com.ecommerce.orderservice.OrderServiceApplication;
import com.ecommerce.orderservice.infrastructure.outbox.OutboxStatus;
import com.ecommerce.orderservice.infrastructure.outbox.SpringDataOutboxRepository;
import com.ecommerce.platform.events.OrderConfirmedEvent;
import com.ecommerce.platform.events.PaymentApprovedEvent;
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
import org.springframework.http.HttpStatus;
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
 * Prova fim a fim do nucleo da Saga (ver CLAUDE.md): criar um pedido persiste a
 * linha correspondente na tabela outbox e o OutboxPublisherWorker a publica de
 * verdade no SNS (LocalStack real via Testcontainers). Tambem simula a chegada de
 * um PaymentApprovedEvent na order-queue - como payment-service ainda nao existe
 * (Marco 10) - para provar que OrderQueueListener confirma o pedido e que a
 * redelivery da mesma mensagem e ignorada pelo Idempotent Consumer Pattern.
 */
@SpringBootTest(classes = OrderServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderFlowIT extends PostgresAndLocalStackTestContainerSupport {

    private static final String ORDER_QUEUE = "order-queue";

    private static final SqsClient SQS_CLIENT = SqsClient.builder()
            .endpointOverride(LOCALSTACK.getEndpointOverride(SQS))
            .region(Region.of(LOCALSTACK.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
            .build();

    private static final String ORDER_QUEUE_URL;

    static {
        // OrderQueueListener resolve a URL da fila no startup do listener SQS,
        // entao ela precisa existir antes do contexto Spring subir.
        ORDER_QUEUE_URL = SQS_CLIENT.createQueue(b -> b.queueName(ORDER_QUEUE)).queueUrl();
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
    private String createOrder(BigDecimal unitPrice) {
        var request = Map.of(
                "customerId", UUID.randomUUID().toString(),
                "items", List.of(Map.of("productId", UUID.randomUUID().toString(), "quantity", 2, "unitPrice", unitPrice)));

        var response = restTemplate.exchange("/orders", HttpMethod.POST, new HttpEntity<>(request, authHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("PENDING");
        return (String) data.get("id");
    }

    @SuppressWarnings("unchecked")
    private String statusOf(String orderId) {
        var response = restTemplate.exchange("/orders/" + orderId, HttpMethod.GET, new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("status");
    }

    @Test
    void createOrderPersistsOutboxRowAndOutboxWorkerPublishesItToRealSns() {
        String orderId = createOrder(new BigDecimal("19.90"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var entry = outboxRepository.findAll().stream()
                    .filter(e -> e.getAggregateId().equals(orderId))
                    .findFirst()
                    .orElseThrow();
            assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(entry.getPublishedAt()).isNotNull();
        });
    }

    @Test
    void orderQueueListenerConfirmsOrderOnPaymentApprovedAndIgnoresRedelivery() {
        String orderId = createOrder(new BigDecimal("19.90"));

        var payload = new PaymentApprovedEvent.Payload(
                UUID.fromString(orderId), UUID.randomUUID(), new BigDecimal("39.80"), Instant.now());
        var event = PaymentApprovedEvent.of(orderId, "corr-it", "trace-it", payload);
        String json = serializer.serialize(event);

        SQS_CLIENT.sendMessage(b -> b.queueUrl(ORDER_QUEUE_URL).messageBody(json));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(statusOf(orderId)).isEqualTo("CONFIRMED"));

        long confirmedEventsBeforeRedelivery = countOrderConfirmedOutboxRows(orderId);
        assertThat(confirmedEventsBeforeRedelivery).isEqualTo(1);

        // Reentrega simulando at-least-once delivery do SQS: mesmo eventId, deve
        // ser ignorado pelo Idempotent Consumer Pattern (processed_events).
        SQS_CLIENT.sendMessage(b -> b.queueUrl(ORDER_QUEUE_URL).messageBody(json));

        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(countOrderConfirmedOutboxRows(orderId)).isEqualTo(1));
    }

    private long countOrderConfirmedOutboxRows(String orderId) {
        return outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals(OrderConfirmedEvent.EVENT_TYPE) && e.getAggregateId().equals(orderId))
                .count();
    }
}
