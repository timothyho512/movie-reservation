package com.example.moviereservation.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventScheduler {

    private final OutboxEventProcessor outboxEventProcessor;

    public OutboxEventScheduler(OutboxEventProcessor outboxEventProcessor) {
        this.outboxEventProcessor = outboxEventProcessor;
    }

    @Scheduled(fixedRate = 30000)
    public void publishDueEvents() {
        outboxEventProcessor.processDueEvents();
    }
}

