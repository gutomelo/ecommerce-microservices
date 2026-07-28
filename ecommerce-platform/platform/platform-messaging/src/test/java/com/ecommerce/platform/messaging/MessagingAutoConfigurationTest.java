package com.ecommerce.platform.messaging;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MessagingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessagingAutoConfiguration.class));

    @Test
    void registersSerializerAndDeserializerWithoutSnsTemplate() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MessageSerializer.class);
            assertThat(context).hasSingleBean(MessageDeserializer.class);
            assertThat(context).doesNotHaveBean(EventPublisher.class);
        });
    }

    @Test
    void registersEventPublisherWhenSnsTemplateIsAvailable() {
        contextRunner.withUserConfiguration(SnsTemplateConfig.class).run(context -> {
            assertThat(context).hasSingleBean(EventPublisher.class);
            assertThat(context.getBean(EventPublisher.class)).isInstanceOf(SnsEventPublisher.class);
        });
    }

    @Test
    void registersEventTypeRouter() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(EventTypeRouter.class));
    }

    static class SnsTemplateConfig {
        @Bean
        SnsTemplate snsTemplate() {
            return mock(SnsTemplate.class);
        }
    }
}
