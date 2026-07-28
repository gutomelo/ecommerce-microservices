package com.ecommerce.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Registra automaticamente as abstracoes de mensageria em qualquer microsservico
 * que dependa de platform-messaging. O publisher so e criado se um SnsTemplate
 * estiver disponivel (servicos somente-consumidores nao precisam publicar).
 */
@AutoConfiguration
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
}
