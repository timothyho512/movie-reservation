package com.example.moviereservation.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class BookingEmailConsumerTest {

    @Test
    void consumesEmailEventPayloadWithoutProviderIntegration() {
        BookingEmailConsumer consumer = new BookingEmailConsumer();

        assertThatCode(() -> consumer.handleEmailEvent(
                "{\"reservationId\":1}",
                "reservation.created",
                "1"
        )).doesNotThrowAnyException();
    }
}
