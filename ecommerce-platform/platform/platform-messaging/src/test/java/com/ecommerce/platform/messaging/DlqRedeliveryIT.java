package com.ecommerce.platform.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Prova, contra um LocalStack real, a regra central de
 * .claude/rules/resiliencia.md: "mensagens que excedem o maximo de tentativas
 * vao automaticamente para a DLQ - nunca descarte mensagens silenciosamente".
 * Um {@code @SqsListener} que sempre lanca excecao nunca confirma (ack) a
 * mensagem; apos {@code maxReceiveCount} redeliveries (redrive policy
 * configurada aqui com os mesmos parametros de
 * infrastructure/localstack/init/init-aws.sh, apenas com valores baixos para o
 * teste rodar rapido), o proprio SQS move a mensagem para a DLQ.
 */
@Testcontainers
@SpringBootTest(classes = DlqRedeliveryIT.AlwaysFailingTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DlqRedeliveryIT {

    private static final String QUEUE_NAME = "dlq-redelivery-it-queue";
    private static final String DLQ_NAME = "dlq-redelivery-it-queue-dlq";
    private static final int MAX_RECEIVE_COUNT = 2;
    private static final int VISIBILITY_TIMEOUT_SECONDS = 1;

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(SQS);

    private static final String QUEUE_URL;
    private static final String DLQ_URL;

    static {
        localstack.start();
        SqsClient bootstrapClient = SqsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(SQS))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .build();

        DLQ_URL = bootstrapClient.createQueue(b -> b.queueName(DLQ_NAME)).queueUrl();
        String dlqArn = bootstrapClient.getQueueAttributes(b -> b.queueUrl(DLQ_URL)
                        .attributeNames(QueueAttributeName.QUEUE_ARN))
                .attributes().get(QueueAttributeName.QUEUE_ARN);

        QUEUE_URL = bootstrapClient.createQueue(b -> b.queueName(QUEUE_NAME)).queueUrl();
        bootstrapClient.setQueueAttributes(b -> b.queueUrl(QUEUE_URL).attributes(Map.of(
                QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(VISIBILITY_TIMEOUT_SECONDS),
                QueueAttributeName.REDRIVE_POLICY,
                "{\"deadLetterTargetArn\":\"" + dlqArn + "\",\"maxReceiveCount\":\"" + MAX_RECEIVE_COUNT + "\"}")));
        bootstrapClient.close();
    }

    @DynamicPropertySource
    static void awsProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", localstack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
        registry.add("spring.cloud.aws.endpoint", () -> localstack.getEndpointOverride(SQS).toString());
    }

    @Test
    void messageThatAlwaysFailsProcessingEndsUpInDlqAfterMaxReceiveCount() {
        SqsClient sqsClient = SqsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(SQS))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .build();

        sqsClient.sendMessage(b -> b.queueUrl(QUEUE_URL).messageBody("mensagem que sempre falha ao processar"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            long dlqMessages = approximateMessageCount(sqsClient, DLQ_URL);
            assertThat(dlqMessages).isEqualTo(1);
        });

        assertThat(approximateMessageCount(sqsClient, QUEUE_URL)).isZero();
        assertThat(AlwaysFailingListener.RECEIVE_COUNT.get()).isGreaterThanOrEqualTo(MAX_RECEIVE_COUNT);

        sqsClient.close();
    }

    private long approximateMessageCount(SqsClient sqsClient, String queueUrl) {
        String value = sqsClient.getQueueAttributes(b -> b.queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES))
                .attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
        return Long.parseLong(value);
    }

    @SpringBootApplication
    static class AlwaysFailingTestApplication {
        static void main(String[] args) {
            SpringApplication.run(AlwaysFailingTestApplication.class, args);
        }

        @Bean
        AlwaysFailingListener alwaysFailingListener() {
            return new AlwaysFailingListener();
        }
    }

    @Component
    static class AlwaysFailingListener {
        static final AtomicInteger RECEIVE_COUNT = new AtomicInteger();

        @SqsListener(QUEUE_NAME)
        void listen(String body) {
            RECEIVE_COUNT.incrementAndGet();
            throw new RuntimeException("Falha simulada de processamento - forca redelivery ate a DLQ");
        }
    }
}
