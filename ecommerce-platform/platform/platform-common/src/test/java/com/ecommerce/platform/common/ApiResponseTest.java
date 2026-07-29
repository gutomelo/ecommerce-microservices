package com.ecommerce.platform.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successWithDataOnly() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.message()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void successWithDataAndMessage() {
        ApiResponse<Integer> response = ApiResponse.success(42, "criado com sucesso");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(42);
        assertThat(response.message()).isEqualTo("criado com sucesso");
    }

    @Test
    void successMessageWithoutData() {
        ApiResponse<Void> response = ApiResponse.successMessage("operacao concluida");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isEqualTo("operacao concluida");
    }
}
