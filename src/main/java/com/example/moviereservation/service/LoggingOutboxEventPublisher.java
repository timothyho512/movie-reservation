package com.example.moviereservation.service;

import com.example.moviereservation.entity.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "false")
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
