package com.ecommerce.notificationservice.application.port;

import com.ecommerce.notificationservice.domain.SmsMessage;

/**
 * Porta de envio de SMS. Sem provedor real localmente - a implementacao apenas
 * loga (ver docs/decisions/0002-mailpit-para-email-local-e-log-para-sms.md).
 */
public interface SmsSender {

    void send(SmsMessage message);
}
