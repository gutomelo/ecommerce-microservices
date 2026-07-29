package com.ecommerce.notificationservice.infrastructure.notification;

import com.ecommerce.notificationservice.domain.SmsMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingSmsSenderTest {

    @Test
    void sendDoesNotThrow() {
        LoggingSmsSender sender = new LoggingSmsSender();

        assertThatCode(() -> sender.send(new SmsMessage("customer-123", "mensagem de teste")))
                .doesNotThrowAnyException();
    }
}
