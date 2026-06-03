package com.example.moviereservation.service;

import com.example.moviereservation.entity.ProcessedOutboxEvent;
import com.example.moviereservation.repository.ProcessedOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerIdempotencyServiceTest {

    private final ProcessedOutboxEventRepository processedOutboxEventRepository =
            mock(ProcessedOutboxEventRepository.class);
    private final ConsumerIdempotencyService consumerIdempotencyService =
            new ConsumerIdempotencyService(processedOutboxEventRepository);

    @Test
    void firstProcessingAttemptRecordsEventAndReturnsTrue() {
        ArgumentCaptor<ProcessedOutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(ProcessedOutboxEvent.class);

        boolean result = consumerIdempotencyService.tryStartProcessing(123L, "booking-email");

        assertThat(result).isTrue();
        verify(processedOutboxEventRepository).saveAndFlush(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(123L);
        assertThat(eventCaptor.getValue().getConsumerName()).isEqualTo("booking-email");
    }

    @Test
    void duplicateProcessingAttemptReturnsFalse() {
        when(processedOutboxEventRepository.saveAndFlush(any(ProcessedOutboxEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        boolean result = consumerIdempotencyService.tryStartProcessing(123L, "booking-email");

        assertThat(result).isFalse();
    }

    @Test
    void differentConsumerNamesAreSavedAsDifferentProcessingRecords() {
        consumerIdempotencyService.tryStartProcessing(123L, "booking-email");
        consumerIdempotencyService.tryStartProcessing(123L, "analytics");

        ArgumentCaptor<ProcessedOutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(ProcessedOutboxEvent.class);
        verify(processedOutboxEventRepository, org.mockito.Mockito.times(2))
                .saveAndFlush(eventCaptor.capture());

        assertThat(eventCaptor.getAllValues())
                .extracting(ProcessedOutboxEvent::getConsumerName)
                .containsExactly("booking-email", "analytics");
    }

    @Test
    void rejectsMissingEventId() {
        assertThatThrownBy(() -> consumerIdempotencyService.tryStartProcessing(null, "booking-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventId is required");
    }
}
