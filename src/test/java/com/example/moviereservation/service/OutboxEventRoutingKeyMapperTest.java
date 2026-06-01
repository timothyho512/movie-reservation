package com.example.moviereservation.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventRoutingKeyMapperTest {

    private final OutboxEventRoutingKeyMapper mapper = new OutboxEventRoutingKeyMapper();

    @Test
    void mapsSupportedOutboxEventTypesToRoutingKeys() {
        assertThat(mapper.routingKeyFor(OutboxEventService.RESERVATION_CREATED))
                .isEqualTo("reservation.created");
        assertThat(mapper.routingKeyFor(OutboxEventService.RESERVATION_CANCELLED))
                .isEqualTo("reservation.cancelled");
        assertThat(mapper.routingKeyFor(OutboxEventService.CHECKOUT_PAYMENT_FINALIZED))
                .isEqualTo("checkout.payment.finalized");
        assertThat(mapper.routingKeyFor(OutboxEventService.CHECKOUT_SESSION_EXPIRED))
                .isEqualTo("checkout.session.expired");
    }

    @Test
    void rejectsUnsupportedOutboxEventTypes() {
        assertThatThrownBy(() -> mapper.routingKeyFor("UnknownEvent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported outbox event type: UnknownEvent");
    }
}
