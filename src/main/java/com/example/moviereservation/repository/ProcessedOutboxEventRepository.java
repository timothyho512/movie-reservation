package com.example.moviereservation.repository;

import com.example.moviereservation.entity.ProcessedOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOutboxEventRepository extends JpaRepository<ProcessedOutboxEvent, Long> {
    boolean existsByEventIdAndConsumerName(Long eventId, String consumerName);
}
