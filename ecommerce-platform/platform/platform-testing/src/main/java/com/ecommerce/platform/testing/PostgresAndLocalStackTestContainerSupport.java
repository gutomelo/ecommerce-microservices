package com.ecommerce.platform.testing;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Base reutilizavel para testes de integracao que precisam de PostgreSQL E
 * SNS/SQS reais ao mesmo tempo (ex.: fluxo completo de outbox + consumo
 * idempotente em order-service/inventory-service/payment-service). Java nao
 * permite herdar de {@link PostgresTestContainerSupport} e
 * {@link LocalStackTestContainerSupport} simultaneamente, entao esta classe
 * reune os dois ciclos de vida (ver .claude/rules/testes.md).
 */
@Testcontainers
public abstract class PostgresAndLocalStackTestContainerSupport {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("platform_test")
            .withUsername("platform_test")
            .withPassword("platform_test");

    protected static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(SNS, SQS);

    static {
        POSTGRES.start();
        LOCALSTACK.start();
    }

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
        registry.add("spring.cloud.aws.endpoint", () -> LOCALSTACK.getEndpointOverride(SNS).toString());
    }
}
