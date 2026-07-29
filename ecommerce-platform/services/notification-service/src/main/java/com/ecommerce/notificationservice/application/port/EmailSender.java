package com.ecommerce.notificationservice.application.port;

import com.ecommerce.notificationservice.domain.EmailMessage;

/**
 * Porta de envio de e-mail. Producao local usa Mailpit (ver
 * docs/decisions/0002-mailpit-para-email-local-e-log-para-sms.md); ao migrar para
 * AWS real, troca-se a implementacao por Amazon SES sem tocar em application/.
 */
public interface EmailSender {

    void send(EmailMessage message);
}
