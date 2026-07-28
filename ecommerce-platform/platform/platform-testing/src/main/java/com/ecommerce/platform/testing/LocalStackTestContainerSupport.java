package com.ecommerce.platform.testing;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Base reutilizavel para testes de integracao que precisam de SNS/SQS reais via
 * LocalStack, com as propriedades spring.cloud.aws.* ja registradas (ver
 * .claude/rules/testes.md e .claude/rules/comunicacao-eventos.md). Container
 * singleton por JVM de teste, mesmo padrao de {@link PostgresTestContainerSupport}.
 */
@Testcontainers
public abstract class LocalStackTestContainerSupport {

    protected static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(SNS, SQS);

    static {
        LOCALSTACK.start();
    }

    @DynamicPropertySource
    static void registerAwsProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
        registry.add("spring.cloud.aws.endpoint", () -> LOCALSTACK.getEndpointOverride(SNS).toString());
    }
}
