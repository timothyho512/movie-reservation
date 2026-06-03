package com.example.moviereservation.service;

import com.example.moviereservation.entity.ProcessedOutboxEvent;
import com.example.moviereservation.repository.ProcessedOutboxEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ConsumerIdempotencyService {

    private final ProcessedOutboxEventRepository processedOutboxEventRepository;

    public ConsumerIdempotencyService(ProcessedOutboxEventRepository processedOutboxEventRepository) {
        this.processedOutboxEventRepository = processedOutboxEventRepository;
    }

    public boolean tryStartProcessing(Long eventId, String consumerName) {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId is required");
        }

        if (consumerName == null || consumerName.isBlank()) {
            throw new IllegalArgumentException("consumerName is required");
        }

        try {
            processedOutboxEventRepository.saveAndFlush(new ProcessedOutboxEvent(eventId, consumerName));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
