package com.example.moviereservation.service;

import com.example.moviereservation.entity.OutboxEvent;
import com.example.moviereservation.entity.OutboxEventStatus;
import com.example.moviereservation.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventProcessorTest {

    private OutboxEventRepository outboxEventRepository;
    private OutboxEventPublisher outboxEventPublisher;
    private OutboxEventProcessor outboxEventProcessor;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        outboxEventPublisher = mock(OutboxEventPublisher.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });

        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        outboxEventProcessor = new OutboxEventProcessor(
                outboxEventRepository,
                outboxEventPublisher,
                transactionTemplate
        );
    }

    @Test
    void processDueEventsPublishesAndMarksEventPublished() {
        OutboxEvent event = new OutboxEvent("ReservationCreated", "Reservation", "1", "{}");

        when(outboxEventRepository.findDueEventsForUpdate(any(LocalDateTime.class), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(outboxEventRepository.saveAll(List.of(event))).thenReturn(List.of(event));
        when(outboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        int processedCount = outboxEventProcessor.processDueEvents();

        assertThat(processedCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxEventPublisher).publish(event);
    }

    @Test
    void processDueEventsRecordsFailureAndSchedulesRetry() {
        OutboxEvent event = new OutboxEvent("ReservationCreated", "Reservation", "1", "{}");

        when(outboxEventRepository.findDueEventsForUpdate(any(LocalDateTime.class), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(outboxEventRepository.saveAll(List.of(event))).thenReturn(List.of(event));
        when(outboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("publisher unavailable"))
                .when(outboxEventPublisher)
                .publish(event);

        int processedCount = outboxEventProcessor.processDueEvents();

        assertThat(processedCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("publisher unavailable");
        assertThat(event.getNextAttemptAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void processDueEventsLeavesEventFailedAfterMaxAttempts() {
        OutboxEvent event = new OutboxEvent("ReservationCreated", "Reservation", "1", "{}");
        event.setAttemptCount(4);
        LocalDateTime originalNextAttemptAt = event.getNextAttemptAt();

        when(outboxEventRepository.findDueEventsForUpdate(any(LocalDateTime.class), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(outboxEventRepository.saveAll(List.of(event))).thenReturn(List.of(event));
        when(outboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("publisher unavailable"))
                .when(outboxEventPublisher)
                .publish(event);

        int processedCount = outboxEventProcessor.processDueEvents();

        assertThat(processedCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getAttemptCount()).isEqualTo(5);
        assertThat(event.getNextAttemptAt()).isEqualTo(originalNextAttemptAt);
    }
}
