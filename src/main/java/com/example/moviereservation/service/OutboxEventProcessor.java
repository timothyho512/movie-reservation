package com.example.moviereservation.service;

import com.example.moviereservation.entity.OutboxEvent;
import com.example.moviereservation.entity.OutboxEventStatus;
import com.example.moviereservation.repository.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxEventProcessor {

    private static final int DEFAULT_BATCH_SIZE = 25;
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final TransactionTemplate transactionTemplate;

    public OutboxEventProcessor(
            OutboxEventRepository outboxEventRepository,
            OutboxEventPublisher outboxEventPublisher,
            TransactionTemplate transactionTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventPublisher = outboxEventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    public int processDueEvents() {
        List<OutboxEvent> events = claimDueEvents(DEFAULT_BATCH_SIZE);

        for (OutboxEvent event : events) {
            try {
                outboxEventPublisher.publish(event);
                markPublished(event.getId());
            } catch (Exception e) {
                markFailed(event.getId(), e);
            }
        }

        return events.size();
    }

    List<OutboxEvent> claimDueEvents(int batchSize) {
        return transactionTemplate.execute(status -> {
            List<OutboxEvent> events = outboxEventRepository.findDueEventsForUpdate(
                    LocalDateTime.now(),
                    MAX_ATTEMPTS,
                    PageRequest.of(0, batchSize)
            );

            for (OutboxEvent event : events) {
                event.setStatus(OutboxEventStatus.PROCESSING);
                event.setLastError(null);
            }

            return outboxEventRepository.saveAll(events);
        });
    }

    private void markPublished(Long eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            OutboxEvent event = outboxEventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventId));
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);
            outboxEventRepository.save(event);
        });
    }

    private void markFailed(Long eventId, Exception exception) {
        transactionTemplate.executeWithoutResult(status -> {
            OutboxEvent event = outboxEventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventId));
            int nextAttemptCount = event.getAttemptCount() + 1;
            event.setAttemptCount(nextAttemptCount);
            event.setLastError(truncateError(exception));
            event.setStatus(OutboxEventStatus.FAILED);

            if (nextAttemptCount < MAX_ATTEMPTS) {
                event.setNextAttemptAt(LocalDateTime.now().plusSeconds(retryDelaySeconds(nextAttemptCount)));
            }

            outboxEventRepository.save(event);
        });
    }

    private long retryDelaySeconds(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> 30;
            case 2 -> 120;
            case 3 -> 300;
            default -> 900;
        };
    }

    private String truncateError(Exception exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getName();
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
