package com.ecommerce.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTypeRouterTest {

    private final EventTypeRouter router = new EventTypeRouter(new ObjectMapper());

    @Test
    void routesMessageToHandlerRegisteredForItsEventType() {
        List<String> receivedByPaymentApproved = new ArrayList<>();
        List<String> receivedByPaymentDeclined = new ArrayList<>();
        router.register("PaymentApproved", receivedByPaymentApproved::add);
        router.register("PaymentDeclined", receivedByPaymentDeclined::add);

        String message = "{\"eventType\":\"PaymentApproved\",\"aggregateId\":\"123\"}";
        router.route(message);

        assertThat(receivedByPaymentApproved).containsExactly(message);
        assertThat(receivedByPaymentDeclined).isEmpty();
    }

    @Test
    void doesNotThrowWhenNoHandlerIsRegistered() {
        List<String> received = new ArrayList<>();
        router.register("PaymentApproved", received::add);

        router.route("{\"eventType\":\"StockUnavailable\"}");

        assertThat(received).isEmpty();
    }

    @Test
    void throwsMessageSerializationExceptionForMalformedJson() {
        assertThatThrownBy(() -> router.route("not-json"))
                .isInstanceOf(MessageSerializationException.class);
    }
}
