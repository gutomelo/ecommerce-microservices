package com.ecommerce.platform.testing;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresAndLocalStackTestContainerSupportIT extends PostgresAndLocalStackTestContainerSupport {

    @Test
    void registerContainerPropertiesExposesAllExpectedKeys() {
        Map<String, Object> resolved = new HashMap<>();
        registerContainerProperties((name, valueSupplier) -> resolved.put(name, valueSupplier.get()));

        assertThat(resolved.get("spring.datasource.url")).isEqualTo(POSTGRES.getJdbcUrl());
        assertThat(resolved.get("spring.datasource.username")).isEqualTo(POSTGRES.getUsername());
        assertThat(resolved.get("spring.datasource.password")).isEqualTo(POSTGRES.getPassword());
        assertThat(resolved.get("spring.cloud.aws.region.static")).isEqualTo(LOCALSTACK.getRegion());
        assertThat(resolved.get("spring.cloud.aws.credentials.access-key")).isEqualTo(LOCALSTACK.getAccessKey());
        assertThat(resolved.get("spring.cloud.aws.credentials.secret-key")).isEqualTo(LOCALSTACK.getSecretKey());
        assertThat(resolved.get("spring.cloud.aws.endpoint"))
                .isEqualTo(LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SNS).toString());
    }

    @Test
    void bothContainersAreRunningAndReachable() throws Exception {
        assertThat(POSTGRES.isRunning()).isTrue();
        assertThat(LOCALSTACK.isRunning()).isTrue();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }

        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));
        try (SnsClient snsClient = SnsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SNS))
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(credentials)
                .build()) {
            var response = snsClient.createTopic(b -> b.name("platform-testing-combined-it"));
            assertThat(response.topicArn()).contains("platform-testing-combined-it");
        }
    }
}
