package com.ecommerce.notificationservice.infrastructure.notification;

import com.ecommerce.notificationservice.application.port.SmsSender;
import com.ecommerce.notificationservice.domain.SmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sem provedor real de SMS localmente - apenas loga (ver
 * docs/decisions/0002-mailpit-para-email-local-e-log-para-sms.md).
 */
@Component
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(SmsMessage message) {
        log.info("SMS para {}: {}", message.to(), message.body());
    }
}
