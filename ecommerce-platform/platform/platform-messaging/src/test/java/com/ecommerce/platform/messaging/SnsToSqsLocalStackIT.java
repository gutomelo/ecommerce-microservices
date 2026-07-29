package com.ecommerce.platform.messaging;

import com.ecommerce.platform.events.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Prova de que SnsEventPublisher realmente publica no SNS, que uma fila SQS
 * inscrita no topico recebe a mensagem (com RawMessageDelivery, formato usado em
 * producao) e que IdempotentEventDispatcher consegue desserializar e processar
 * essa mensagem real - fim a fim, contra um LocalStack de verdade via Testcontainers.
 */
@Testcontainers
class SnsToSqsLocalStackIT {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LocalStackContainer.Service.SNS, LocalStackContainer.Service.SQS);

    private SnsClient snsClient;
    private SqsClient sqsClient;
    private String topicArn;
    private String queueUrl;

    @BeforeEach
    void setUp() {
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
        Region region = Region.of(localstack.getRegion());

        snsClient = SnsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SNS))
                .region(region)
                .credentialsProvider(credentials)
                .build();

        sqsClient = SqsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
                .region(region)
                .credentialsProvider(credentials)
                .build();

        topicArn = snsClient.createTopic(b -> b.name("order-created-it")).topicArn();
        queueUrl = sqsClient.createQueue(b -> b.queueName("order-created-it-queue")).queueUrl();
        String queueArn = sqsClient.getQueueAttributes(b -> b.queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN))
                .attributes().get(QueueAttributeName.QUEUE_ARN);

        String subscriptionArn = snsClient.subscribe(b -> b.topicArn(topicArn)
                        .protocol("sqs")
                        .endpoint(queueArn)
                        .returnSubscriptionArn(true))
                .subscriptionArn();
        snsClient.setSubscriptionAttributes(b -> b.subscriptionArn(subscriptionArn)
                .attributeName("RawMessageDelivery")
                .attributeValue("true"));
    }

    @AfterEach
    void tearDown() {
        snsClient.close();
        sqsClient.close();
    }

    @Test
    void publishedEventIsReceivedByQueueAndDispatchedIdempotently() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MessageSerializer serializer = new JacksonMessageSerializer(objectMapper);
        MessageDeserializer deserializer = new JacksonMessageDeserializer(objectMapper);

        SnsTemplate snsTemplate = new SnsTemplate(snsClient);
        MessagingProperties properties = new MessagingProperties();
        EventPublisher publisher = new SnsEventPublisher(snsTemplate, serializer, properties);

        OrderCreatedEvent event = OrderCreatedEvent.of(
                UUID.randomUUID().toString(), "corr-it", "trace-it",
                new OrderCreatedEvent.Payload(
                        UUID.randomUUID(), UUID.randomUUID(),
                        List.of(new OrderCreatedEvent.Payload.Item(UUID.randomUUID(), 2, new BigDecimal("19.90"))),
                        new BigDecimal("39.80")));

        publisher.publish(topicArn, event);

        Message received = receiveSingleMessage();
        assertThat(received.body()).contains(event.getEventId().toString());

        AtomicBoolean consumed = new AtomicBoolean(false);
        InMemoryProcessedEventChecker processedEventChecker = new InMemoryProcessedEventChecker();
        EventConsumer<OrderCreatedEvent> consumer = e -> consumed.set(true);
        IdempotentEventDispatcher<OrderCreatedEvent> dispatcher = new IdempotentEventDispatcher<>(
                deserializer, processedEventChecker, consumer, OrderCreatedEvent.class);

        dispatcher.dispatch(received.body());
        assertThat(consumed).isTrue();

        // Reentrega simulando at-least-once delivery do SQS: idempotencia impede reprocessamento
        consumed.set(false);
        dispatcher.dispatch(received.body());
        assertThat(consumed).isFalse();
    }

    private Message receiveSingleMessage() {
        return await().atMost(Duration.ofSeconds(15)).until(() -> {
            var messages = sqsClient.receiveMessage(b -> b.queueUrl(queueUrl).maxNumberOfMessages(1).waitTimeSeconds(2))
                    .messages();
            return messages.isEmpty() ? null : messages.get(0);
        }, message -> message != null);
    }

    static class InMemoryProcessedEventChecker implements ProcessedEventChecker {
        private final Map<UUID, Boolean> processed = new java.util.HashMap<>();

        @Override
        public boolean isProcessed(UUID eventId) {
            return processed.containsKey(eventId);
        }

        @Override
        public void markProcessed(UUID eventId) {
            processed.put(eventId, true);
        }
    }
}
