package com.ecommerce.notificationservice.infrastructure.notification;

import com.ecommerce.notificationservice.domain.EmailMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JavaMailEmailSenderTest {

    @Test
    void sendsSimpleMailMessageWithExpectedFields() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        JavaMailEmailSender sender = new JavaMailEmailSender(javaMailSender);

        sender.send(new EmailMessage("customer@example.com", "Assunto", "Corpo da mensagem"));

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("customer@example.com");
        assertThat(sent.getSubject()).isEqualTo("Assunto");
        assertThat(sent.getText()).isEqualTo("Corpo da mensagem");
    }
}
