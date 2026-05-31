package com.example.moviereservation.service;

import com.example.moviereservation.entity.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOutboxEventPublisher implements OutboxEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LoggingOutboxEventPublisher.class);

    @Override
    public void publish(OutboxEvent event) {
        logger.info(
                "Publishing outbox event id={} type={} aggregateType={} aggregateId={} payload={}",
                event.getId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getPayloadJson()
        );
    }
}
