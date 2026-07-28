package com.ecommerce.platform.testing;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStackTestContainerSupportIT extends LocalStackTestContainerSupport {

    @Test
    void containerIsRunningAndSnsIsReachable() {
        assertThat(LOCALSTACK.isRunning()).isTrue();

        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()));

        try (SnsClient snsClient = SnsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SNS))
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(credentials)
                .build()) {
            var response = snsClient.createTopic(b -> b.name("platform-testing-it"));
            assertThat(response.topicArn()).contains("platform-testing-it");
        }
    }
}
