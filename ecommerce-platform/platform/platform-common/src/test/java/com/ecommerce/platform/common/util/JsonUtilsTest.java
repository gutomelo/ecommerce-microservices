package com.ecommerce.platform.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonUtilsTest {

    record Sample(String name, int quantity) {
    }

    @Test
    void toJsonAndFromJsonRoundTrip() {
        Sample sample = new Sample("widget", 3);

        String json = JsonUtils.toJson(sample);
        Sample parsed = JsonUtils.fromJson(json, Sample.class);

        assertThat(json).contains("\"widget\"");
        assertThat(parsed).isEqualTo(sample);
    }

    @Test
    void toJsonWithNullReturnsNull() {
        assertThat(JsonUtils.toJson(null)).isNull();
    }

    @Test
    void fromJsonWithNullOrBlankReturnsNull() {
        assertThat(JsonUtils.fromJson(null, Sample.class)).isNull();
        assertThat(JsonUtils.fromJson("  ", Sample.class)).isNull();
    }

    @Test
    void fromJsonWithInvalidJsonThrows() {
        assertThatThrownBy(() -> JsonUtils.fromJson("{not-valid", Sample.class))
                .isInstanceOf(JsonUtils.JsonProcessingRuntimeException.class);
    }

    @Test
    void mapperReturnsSharedInstance() {
        assertThat(JsonUtils.mapper()).isNotNull();
    }
}
