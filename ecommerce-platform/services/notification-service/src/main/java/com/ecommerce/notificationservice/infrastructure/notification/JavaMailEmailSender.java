package com.ecommerce.notificationservice.infrastructure.notification;

import com.ecommerce.notificationservice.application.port.EmailSender;
import com.ecommerce.notificationservice.domain.EmailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class JavaMailEmailSender implements EmailSender {

    private static final String FROM_ADDRESS = "no-reply@ecommerce-platform.local";

    private final JavaMailSender javaMailSender;

    public JavaMailEmailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(FROM_ADDRESS);
        mailMessage.setTo(message.to());
        mailMessage.setSubject(message.subject());
        mailMessage.setText(message.body());
        javaMailSender.send(mailMessage);
    }
}
