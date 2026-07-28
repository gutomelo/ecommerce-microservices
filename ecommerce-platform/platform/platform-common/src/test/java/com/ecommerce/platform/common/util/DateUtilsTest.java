package com.ecommerce.platform.common.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    @Test
    void nowUtcReturnsCurrentInstant() {
        Instant before = Instant.now().minusSeconds(1);
        Instant now = DateUtils.nowUtc();
        Instant after = Instant.now().plusSeconds(1);

        assertThat(now).isBetween(before, after);
    }

    @Test
    void formatAndParseRoundTrip() {
        Instant instant = Instant.parse("2026-07-28T12:00:00Z");

        String formatted = DateUtils.formatIsoUtc(instant);
        Instant parsed = DateUtils.parseIsoUtc(formatted);

        assertThat(parsed).isEqualTo(instant);
    }

    @Test
    void formatAndParseHandleNullAndBlank() {
        assertThat(DateUtils.formatIsoUtc(null)).isNull();
        assertThat(DateUtils.parseIsoUtc(null)).isNull();
        assertThat(DateUtils.parseIsoUtc("  ")).isNull();
    }

    @Test
    void isBeforeAndIsAfter() {
        Instant earlier = Instant.parse("2026-01-01T00:00:00Z");
        Instant later = Instant.parse("2026-06-01T00:00:00Z");

        assertThat(DateUtils.isBefore(earlier, later)).isTrue();
        assertThat(DateUtils.isAfter(later, earlier)).isTrue();
        assertThat(DateUtils.isBefore(later, earlier)).isFalse();
        assertThat(DateUtils.isBefore(null, later)).isFalse();
        assertThat(DateUtils.isAfter(later, null)).isFalse();
    }
}
