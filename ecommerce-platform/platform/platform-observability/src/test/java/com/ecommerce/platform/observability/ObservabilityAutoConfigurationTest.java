package com.ecommerce.platform.observability;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class));

    @Test
    void registersFilterAndRegistrationBeanWithoutTracer() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CorrelationIdFilter.class);
            assertThat(context).hasSingleBean(FilterRegistrationBean.class);

            @SuppressWarnings("unchecked")
            FilterRegistrationBean<CorrelationIdFilter> registration =
                    context.getBean(FilterRegistrationBean.class);
            assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        });
    }

    @Test
    void registersFilterWhenTracerIsAvailable() {
        contextRunner.withUserConfiguration(TracerConfig.class).run(context ->
                assertThat(context).hasSingleBean(CorrelationIdFilter.class));
    }

    @Test
    void respectsExistingUserDefinedFilter() {
        contextRunner.withUserConfiguration(CustomFilterConfig.class).run(context ->
                assertThat(context.getBean(CorrelationIdFilter.class))
                        .isSameAs(CustomFilterConfig.CUSTOM_INSTANCE));
    }

    static class TracerConfig {
        @Bean
        Tracer tracer() {
            return mock(Tracer.class);
        }
    }

    static class CustomFilterConfig {
        static CorrelationIdFilter CUSTOM_INSTANCE = new CorrelationIdFilter(null);

        @Bean
        CorrelationIdFilter correlationIdFilter() {
            return CUSTOM_INSTANCE;
        }
    }
}
