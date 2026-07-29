package com.ecommerce.platform.exception;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExceptionAutoConfiguration.class));

    @Test
    void registersGlobalExceptionHandler() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
    }

    @Test
    void respectsExistingUserDefinedBean() {
        contextRunner.withUserConfiguration(CustomHandlerConfig.class).run(context ->
                assertThat(context.getBean(GlobalExceptionHandler.class)).isSameAs(CustomHandlerConfig.CUSTOM_INSTANCE));
    }

    static class CustomHandlerConfig {
        static final GlobalExceptionHandler CUSTOM_INSTANCE = new GlobalExceptionHandler();

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return CUSTOM_INSTANCE;
        }
    }
}
