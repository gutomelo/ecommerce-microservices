package com.ecommerce.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JwtAutoConfiguration.class));

    @Test
    void registersBeansWhenSecretIsConfigured() {
        contextRunner
                .withPropertyValues("platform.security.jwt.secret=test-secret-key-with-at-least-32-characters!!")
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtTokenProvider.class);
                    assertThat(context).hasSingleBean(JwtAuthenticationFilter.class);
                    assertThat(context).hasSingleBean(JwtProperties.class);
                });
    }

    @Test
    void respectsExistingUserDefinedBean() {
        contextRunner
                .withPropertyValues("platform.security.jwt.secret=test-secret-key-with-at-least-32-characters!!")
                .withUserConfiguration(CustomTokenProviderConfig.class)
                .run(context -> assertThat(context.getBean(JwtTokenProvider.class))
                        .isSameAs(CustomTokenProviderConfig.CUSTOM_INSTANCE));
    }

    static class CustomTokenProviderConfig {
        static JwtTokenProvider CUSTOM_INSTANCE;

        @org.springframework.context.annotation.Bean
        JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
            CUSTOM_INSTANCE = new JwtTokenProvider(properties);
            return CUSTOM_INSTANCE;
        }
    }
}
