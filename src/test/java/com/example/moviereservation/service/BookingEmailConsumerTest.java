package com.example.moviereservation.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingEmailConsumerTest {

    @Test
    void consumesEmailEventPayloadWithoutProviderIntegration() {
        ConsumerIdempotencyService consumerIdempotencyService = mock(ConsumerIdempotencyService.class);
        when(consumerIdempotencyService.tryStartProcessing(1L, BookingEmailConsumer.CONSUMER_NAME))
                .thenReturn(true);
        BookingEmailConsumer consumer = new BookingEmailConsumer(consumerIdempotencyService);

        assertThatCode(() -> consumer.handleEmailEvent(
                "{\"reservationId\":1}",
                1L,
                "reservation.created",
                "1"
        )).doesNotThrowAnyException();

        verify(consumerIdempotencyService).tryStartProcessing(1L, BookingEmailConsumer.CONSUMER_NAME);
    }

    @Test
    void skipsDuplicateEmailEvent() {
        ConsumerIdempotencyService consumerIdempotencyService = mock(ConsumerIdempotencyService.class);
        when(consumerIdempotencyService.tryStartProcessing(1L, BookingEmailConsumer.CONSUMER_NAME))
                .thenReturn(true)
                .thenReturn(false);
        BookingEmailConsumer consumer = new BookingEmailConsumer(consumerIdempotencyService);

        consumer.handleEmailEvent("{\"reservationId\":1}", 1L, "reservation.created", "1");
        consumer.handleEmailEvent("{\"reservationId\":1}", 1L, "reservation.created", "1");

        verify(consumerIdempotencyService, times(2))
                .tryStartProcessing(1L, BookingEmailConsumer.CONSUMER_NAME);
    }

    @Test
    void missingEventIdStillProcessesWithoutIdempotencyGuard() {
        ConsumerIdempotencyService consumerIdempotencyService = mock(ConsumerIdempotencyService.class);
        BookingEmailConsumer consumer = new BookingEmailConsumer(consumerIdempotencyService);

        assertThatCode(() -> consumer.handleEmailEvent(
                "{\"reservationId\":1}",
                null,
                "reservation.created",
                "1"
        )).doesNotThrowAnyException();

        verify(consumerIdempotencyService, times(0))
                .tryStartProcessing(1L, BookingEmailConsumer.CONSUMER_NAME);
    }
}
