package com.ecommerce.notificationservice.application;

import com.ecommerce.notificationservice.application.port.EmailSender;
import com.ecommerce.notificationservice.application.port.SmsSender;
import com.ecommerce.notificationservice.domain.EmailMessage;
import com.ecommerce.notificationservice.domain.SmsMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    private EmailSender emailSender;
    private SmsSender smsSender;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        emailSender = mock(EmailSender.class);
        smsSender = mock(SmsSender.class);
        notificationService = new NotificationService(emailSender, smsSender);
    }

    @Test
    void notifyOrderCreatedSendsEmailOnly() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        notificationService.notifyOrderCreated(orderId, customerId);

        verify(emailSender).send(any(EmailMessage.class));
        verify(smsSender, never()).send(any());
    }

    @Test
    void notifyOrderConfirmedSendsEmailAndSms() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        notificationService.notifyOrderConfirmed(orderId, customerId);

        verify(emailSender).send(any(EmailMessage.class));
        verify(smsSender).send(any(SmsMessage.class));
    }

    @Test
    void notifyOrderCancelledSendsEmailAndSmsWithReason() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        notificationService.notifyOrderCancelled(orderId, customerId, "estoque insuficiente");

        var emailCaptor = org.mockito.ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(emailCaptor.capture());
        assertThat(emailCaptor.getValue().body()).contains("estoque insuficiente");

        var smsCaptor = org.mockito.ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsSender).send(smsCaptor.capture());
        assertThat(smsCaptor.getValue().body()).contains("estoque insuficiente");
    }

    @Test
    void logIntermediateSagaEventDoesNotSendEmailOrSms() {
        notificationService.logIntermediateSagaEvent("StockReserved", UUID.randomUUID().toString());

        verify(emailSender, never()).send(any());
        verify(smsSender, never()).send(any());
    }
}
