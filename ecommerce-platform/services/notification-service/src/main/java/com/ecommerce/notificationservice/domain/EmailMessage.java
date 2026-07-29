package com.ecommerce.notificationservice.domain;

public record EmailMessage(String to, String subject, String body) {
}
