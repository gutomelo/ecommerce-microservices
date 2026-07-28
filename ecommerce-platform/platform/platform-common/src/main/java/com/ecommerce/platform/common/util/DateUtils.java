package com.ecommerce.platform.common.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Utilitarios de data/hora compartilhados. Toda a plataforma trabalha em UTC
 * internamente; conversao para fuso local e responsabilidade da camada de apresentacao.
 */
public final class DateUtils {

    public static final DateTimeFormatter ISO_UTC = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private DateUtils() {
    }

    public static Instant nowUtc() {
        return Instant.now();
    }

    public static String formatIsoUtc(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_UTC.format(instant);
    }

    public static Instant parseIsoUtc(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }

    public static boolean isBefore(Instant reference, Instant other) {
        return reference != null && other != null && reference.isBefore(other);
    }

    public static boolean isAfter(Instant reference, Instant other) {
        return reference != null && other != null && reference.isAfter(other);
    }
}
