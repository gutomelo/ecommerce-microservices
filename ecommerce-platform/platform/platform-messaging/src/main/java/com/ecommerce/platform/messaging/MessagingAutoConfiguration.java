package com.ecommerce.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.awspring.cloud.autoconfigure.sns.SnsAutoConfiguration;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Registra automaticamente as abstracoes de mensageria em qualquer microsservico
 * que dependa de platform-messaging. O publisher so e criado se um SnsTemplate
 * estiver disponivel (servicos somente-consumidores nao precisam publicar).
 * <p>
 * {@code @AutoConfigureAfter(SnsAutoConfiguration.class)} e obrigatorio: sem ele,
 * a ordem de avaliacao entre autoconfiguracoes de jars diferentes nao e garantida,
 * e o {@code @ConditionalOnBean(SnsTemplate.class)} abaixo pode ser avaliado antes
 * do SnsTemplate ainda existir no contexto - falhando silenciosamente em nao criar
 * o EventPublisher mesmo com spring-cloud-aws-starter-sns no classpath.
 */
@AutoConfiguration
@AutoConfigureAfter(SnsAutoConfiguration.class)
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper platformMessagingObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageSerializer messageSerializer(ObjectMapper objectMapper) {
        return new JacksonMessageSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageDeserializer messageDeserializer(ObjectMapper objectMapper) {
        return new JacksonMessageDeserializer(objectMapper);
    }

    @Bean
    @ConditionalOnBean(SnsTemplate.class)
    @ConditionalOnMissingBean
    public EventPublisher eventPublisher(SnsTemplate snsTemplate, MessageSerializer serializer,
                                          MessagingProperties properties) {
        return new SnsEventPublisher(snsTemplate, serializer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventTypeRouter eventTypeRouter(ObjectMapper objectMapper) {
        return new EventTypeRouter(objectMapper);
    }
}
