package com.ecommerce.notificationservice.application;

import com.ecommerce.notificationservice.application.port.EmailSender;
import com.ecommerce.notificationservice.application.port.SmsSender;
import com.ecommerce.notificationservice.domain.EmailMessage;
import com.ecommerce.notificationservice.domain.SmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Reage aos 7 eventos do catalogo (ver docs/events/catalogo-eventos.md).
 * <p>
 * Apenas OrderCreatedEvent, OrderConfirmedEvent e OrderCancelledEvent carregam
 * customerId no payload - os demais (StockReserved/StockUnavailable/
 * PaymentApproved/PaymentDeclined) sao passos intermediarios da Saga, sem
 * destinatario conhecido aqui (ver .claude/rules/arquitetura.md: nenhuma chamada
 * sincrona entre servicos para buscar o cliente). O desfecho de uma falha
 * intermediaria (estoque indisponivel ou pagamento recusado) sempre chega ao
 * cliente via OrderCancelledEvent, que carrega tanto customerId quanto o motivo
 * (copiado por order-service) - por isso so os tres eventos com customerId geram
 * notificacao; os demais apenas sao logados para rastreabilidade.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public NotificationService(EmailSender emailSender, SmsSender smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    public void notifyOrderCreated(UUID orderId, UUID customerId) {
        emailSender.send(new EmailMessage(emailFor(customerId), "Recebemos seu pedido",
                "Seu pedido %s foi recebido e esta sendo processado.".formatted(orderId)));
    }

    public void notifyOrderConfirmed(UUID orderId, UUID customerId) {
        emailSender.send(new EmailMessage(emailFor(customerId), "Pedido confirmado",
                "Seu pedido %s foi confirmado! Pagamento aprovado e estoque reservado.".formatted(orderId)));
        smsSender.send(new SmsMessage(phoneFor(customerId), "Pedido %s confirmado.".formatted(orderId)));
    }

    public void notifyOrderCancelled(UUID orderId, UUID customerId, String reason) {
        emailSender.send(new EmailMessage(emailFor(customerId), "Pedido cancelado",
                "Seu pedido %s foi cancelado. Motivo: %s.".formatted(orderId, reason)));
        smsSender.send(new SmsMessage(phoneFor(customerId), "Pedido %s cancelado: %s.".formatted(orderId, reason)));
    }

    public void logIntermediateSagaEvent(String eventType, String aggregateId) {
        log.info("Evento intermediario da Saga observado: eventType={}, aggregateId={} (sem notificacao direta)",
                eventType, aggregateId);
    }

    /**
     * Sem acesso ao banco de customer-service (cada servico tem seu proprio
     * banco - ver .claude/rules/banco-de-dados.md) e sem chamada sincrona entre
     * servicos (ver .claude/rules/arquitetura.md), o endereco e derivado
     * deterministicamente do customerId. Mailpit aceita qualquer destinatario -
     * o objetivo aqui e provar o pipeline evento -> e-mail, nao a integracao
     * real com o cadastro do cliente (que exigiria um provedor real como SES).
     */
    private String emailFor(UUID customerId) {
        return "customer-" + customerId + "@ecommerce-platform.local";
    }

    private String phoneFor(UUID customerId) {
        return "customer-" + customerId;
    }
}
