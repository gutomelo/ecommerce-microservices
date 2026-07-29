package com.ecommerce.notificationservice.api;

import com.ecommerce.notificationservice.NotificationServiceApplication;
import com.ecommerce.notificationservice.application.port.EmailSender;
import com.ecommerce.notificationservice.application.port.SmsSender;
import com.ecommerce.notificationservice.domain.EmailMessage;
import com.ecommerce.platform.events.OrderCancelledEvent;
import com.ecommerce.platform.events.OrderConfirmedEvent;
import com.ecommerce.platform.events.OrderCreatedEvent;
import com.ecommerce.platform.events.PaymentApprovedEvent;
import com.ecommerce.platform.events.PaymentDeclinedEvent;
import com.ecommerce.platform.events.StockReservedEvent;
import com.ecommerce.platform.events.StockUnavailableEvent;
import com.ecommerce.platform.messaging.MessageSerializer;
import com.ecommerce.platform.testing.PostgresAndLocalStackTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Prova fim a fim do notification-service (ver CLAUDE.md): simula os 7 eventos do
 * catalogo chegando na notification-queue (fan-out de todos os topicos - ver
 * infrastructure/localstack/init/init-aws.sh) contra LocalStack/Postgres reais, e
 * verifica que so os 3 eventos com customerId (OrderCreated/OrderConfirmed/
 * OrderCancelled) geram e-mail/SMS, os demais apenas sao observados, e que a
 * redelivery e ignorada pela idempotencia. EmailSender/SmsSender sao substituidos
 * por mocks (@MockitoBean): a entrega real via Mailpit e testada separadamente em
 * JavaMailEmailSenderTest (unitario) e na validacao via docker compose - aqui o
 * que se prova e a integracao SQS -> EventTypeRouter -> IdempotentEventDispatcher
 * -> NotificationService.
 */
@SpringBootTest(classes = NotificationServiceApplication.class)
class NotificationFlowIT extends PostgresAndLocalStackTestContainerSupport {

    private static final String NOTIFICATION_QUEUE = "notification-queue";

    private static final SqsClient SQS_CLIENT = SqsClient.builder()
            .endpointOverride(LOCALSTACK.getEndpointOverride(SQS))
            .region(Region.of(LOCALSTACK.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
            .build();

    private static final String NOTIFICATION_QUEUE_URL;

    static {
        NOTIFICATION_QUEUE_URL = SQS_CLIENT.createQueue(b -> b.queueName(NOTIFICATION_QUEUE)).queueUrl();
    }

    @Autowired
    private MessageSerializer serializer;

    @MockitoBean
    private EmailSender emailSender;

    @MockitoBean
    private SmsSender smsSender;

    private void send(Object event) {
        SQS_CLIENT.sendMessage(b -> b.queueUrl(NOTIFICATION_QUEUE_URL)
                .messageBody(serializer.serialize(event)));
    }

    @Test
    void orderCreatedTriggersEmailButNotSms() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var payload = new OrderCreatedEvent.Payload(orderId, customerId,
                List.of(new OrderCreatedEvent.Payload.Item(UUID.randomUUID(), 1, BigDecimal.TEN)), BigDecimal.TEN);
        send(OrderCreatedEvent.of(orderId.toString(), "corr-it", "trace-it", payload));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                verify(emailSender, times(1)).send(any(EmailMessage.class)));
        verify(smsSender, never()).send(any());
    }

    @Test
    void orderConfirmedTriggersEmailAndSms() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var payload = new OrderConfirmedEvent.Payload(orderId, customerId, Instant.now());
        send(OrderConfirmedEvent.of(orderId.toString(), "corr-it", "trace-it", payload));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(emailSender, times(1)).send(any(EmailMessage.class));
            verify(smsSender, times(1)).send(any());
        });
    }

    @Test
    void orderCancelledTriggersEmailAndSmsWithReason() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var payload = new OrderCancelledEvent.Payload(orderId, customerId, "estoque insuficiente", Instant.now());
        send(OrderCancelledEvent.of(orderId.toString(), "corr-it", "trace-it", payload));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(emailSender, times(1)).send(any(EmailMessage.class));
            verify(smsSender, times(1)).send(any());
        });
    }

    @Test
    void intermediateSagaEventsDoNotTriggerEmailOrSms() {
        UUID orderId = UUID.randomUUID();
        send(StockReservedEvent.of(orderId.toString(), "corr-it", "trace-it",
                new StockReservedEvent.Payload(orderId,
                        List.of(new StockReservedEvent.Payload.ReservedItem(UUID.randomUUID(), 1)), BigDecimal.TEN)));
        send(StockUnavailableEvent.of(orderId.toString(), "corr-it", "trace-it",
                new StockUnavailableEvent.Payload(orderId, List.of(UUID.randomUUID()), "sem estoque")));
        send(PaymentApprovedEvent.of(orderId.toString(), "corr-it", "trace-it",
                new PaymentApprovedEvent.Payload(orderId, UUID.randomUUID(), BigDecimal.TEN, Instant.now())));
        send(PaymentDeclinedEvent.of(orderId.toString(), "corr-it", "trace-it",
                new PaymentDeclinedEvent.Payload(orderId, BigDecimal.TEN, "saldo insuficiente")));

        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(emailSender, never()).send(any());
            verify(smsSender, never()).send(any());
        });
    }

    @Test
    void redeliveryOfSameMessageIsIgnoredByIdempotency() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var payload = new OrderConfirmedEvent.Payload(orderId, customerId, Instant.now());
        var event = OrderConfirmedEvent.of(orderId.toString(), "corr-it", "trace-it", payload);
        String messageBody = serializer.serialize(event);

        SQS_CLIENT.sendMessage(b -> b.queueUrl(NOTIFICATION_QUEUE_URL).messageBody(messageBody));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                verify(emailSender, times(1)).send(any(EmailMessage.class)));

        SQS_CLIENT.sendMessage(b -> b.queueUrl(NOTIFICATION_QUEUE_URL).messageBody(messageBody));

        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                verify(emailSender, times(1)).send(any(EmailMessage.class)));
    }
}
