package com.ecommerce.platform.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato base de todo evento de dominio publicado na plataforma. Imutavel e
 * serializado em JSON. Nenhum microsservico deve criar sua propria versao de um
 * evento existente (ver .claude/rules/comunicacao-eventos.md).
 * <p>
 * {@code @Jacksonized} fica apenas nas subclasses concretas (o Jackson sempre
 * desserializa para o tipo concreto do evento, nunca para BaseEvent diretamente).
 *
 * @param <T> tipo do payload especifico do evento (definido por cada subclasse).
 */
@Getter
@SuperBuilder
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseEvent<T> {

    private final UUID eventId;
    private final String aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final Instant occurredAt;
    private final String correlationId;
    private final String traceId;
    private final int version;
    private final T payload;
}
