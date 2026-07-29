package com.ecommerce.paymentservice.api;

import com.ecommerce.paymentservice.PaymentServiceApplication;
import com.ecommerce.paymentservice.infrastructure.outbox.OutboxStatus;
import com.ecommerce.paymentservice.infrastructure.outbox.SpringDataOutboxRepository;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import com.ecommerce.platform.events.StockReservedEvent;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Prova fim a fim da decisao de pagamento simulada (ver CLAUDE.md): simula
 * StockReservedEvent chegando na payment-queue (como inventory-service faria via
 * SNS - ver infrastructure/localstack/init/init-aws.sh) e verifica aprovacao,
 * recusa e idempotencia contra LocalStack/Postgres reais.
 */
@SpringBootTest(classes = PaymentServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentFlowIT extends PostgresAndLocalStackTestContainerSupport {

    private static final String PAYMENT_QUEUE = "payment-queue";

    private static final SqsClient SQS_CLIENT = SqsClient.builder()
            .endpointOverride(LOCALSTACK.getEndpointOverride(SQS))
            .region(Region.of(LOCALSTACK.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
            .build();

    private static final String PAYMENT_QUEUE_URL;

    static {
        PAYMENT_QUEUE_URL = SQS_CLIENT.createQueue(b -> b.queueName(PAYMENT_QUEUE)).queueUrl();
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

    private String stockReservedJson(UUID orderId, BigDecimal amount) {
        var payload = new StockReservedEvent.Payload(orderId,
                List.of(new StockReservedEvent.Payload.ReservedItem(UUID.randomUUID(), 2)), amount);
        var event = StockReservedEvent.of(orderId.toString(), "corr-it", "trace-it", payload);
        return serializer.serialize(event);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> paymentsForOrder(UUID orderId) {
        var response = restTemplate.exchange("/payments/order/" + orderId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), new ParameterizedTypeReference<Map<String, Object>>() {
                });
        return (List<Map<String, Object>>) response.getBody().get("data");
    }

    private long countOutboxRowsForOrder(String eventType, UUID orderId) {
        return outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals(eventType) && e.getAggregateId().equals(orderId.toString()))
                .count();
    }

    @Test
    void approvesPaymentAndPublishesPaymentApprovedWhenBelowThreshold() {
        UUID orderId = UUID.randomUUID();
        SQS_CLIENT.sendMessage(b -> b.queueUrl(PAYMENT_QUEUE_URL).messageBody(stockReservedJson(orderId, new BigDecimal("100.00"))));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var payments = paymentsForOrder(orderId);
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).get("status")).isEqualTo("APPROVED");
        });
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var entry = outboxRepository.findAll().stream()
                    .filter(e -> e.getEventType().equals(PaymentApprovedEvent.EVENT_TYPE)
                            && e.getAggregateId().equals(orderId.toString()))
                    .findFirst().orElseThrow();
            assertThat(entry.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        });
    }

    @Test
    void declinesPaymentAndPublishesPaymentDeclinedWhenAboveThreshold() {
        UUID orderId = UUID.randomUUID();
        SQS_CLIENT.sendMessage(b -> b.queueUrl(PAYMENT_QUEUE_URL).messageBody(stockReservedJson(orderId, new BigDecimal("999.99"))));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var payments = paymentsForOrder(orderId);
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).get("status")).isEqualTo("DECLINED");
        });
        assertThat(countOutboxRowsForOrder(PaymentDeclinedEvent.EVENT_TYPE, orderId)).isEqualTo(1);
    }

    @Test
    void redeliveryOfSameMessageIsIgnoredByIdempotency() {
        UUID orderId = UUID.randomUUID();
        String messageBody = stockReservedJson(orderId, new BigDecimal("100.00"));

        SQS_CLIENT.sendMessage(b -> b.queueUrl(PAYMENT_QUEUE_URL).messageBody(messageBody));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(paymentsForOrder(orderId)).hasSize(1));

        SQS_CLIENT.sendMessage(b -> b.queueUrl(PAYMENT_QUEUE_URL).messageBody(messageBody));

        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(paymentsForOrder(orderId)).hasSize(1));
    }
}
