package com.ecommerce.platform.testing;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base reutilizavel para testes de integracao que precisam de um PostgreSQL real.
 * Evita que cada microsservico reimplemente o ciclo de vida do container e a
 * configuracao de spring.datasource.* (ver .claude/rules/testes.md).
 * <p>
 * O container e um singleton por JVM de teste (inicia uma vez, nunca e parado
 * explicitamente - o Ryuk do Testcontainers encerra ao final da JVM), conforme o
 * padrao recomendado pelo proprio Testcontainers para compartilhar o container
 * entre varias classes de teste.
 */
@Testcontainers
public abstract class PostgresTestContainerSupport {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("platform_test")
            .withUsername("platform_test")
            .withPassword("platform_test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
