package com.example.moviereservation.service;

import com.example.moviereservation.config.RabbitMqOutboxConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class BookingEmailConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BookingEmailConsumer.class);
    static final String CONSUMER_NAME = "booking-email";

    private final ConsumerIdempotencyService consumerIdempotencyService;

    public BookingEmailConsumer(ConsumerIdempotencyService consumerIdempotencyService) {
        this.consumerIdempotencyService = consumerIdempotencyService;
    }

    @RabbitListener(queues = RabbitMqOutboxConfig.EMAIL_QUEUE)
    public void handleEmailEvent(
            String payload,
            @Header(name = "eventId", required = false) Long eventId,
            @Header(name = "eventType", required = false) String eventType,
            @Header(name = "aggregateId", required = false) String aggregateId
    ) {
        if (eventId == null) {
            logger.warn(
                    "Processing email event without idempotency because eventId header is missing eventType={} aggregateId={}",
                    eventType,
                    aggregateId
            );
            processEmailEvent(payload, eventType, aggregateId);
            return;
        }

        if (!consumerIdempotencyService.tryStartProcessing(eventId, CONSUMER_NAME)) {
            logger.info(
                    "Skipping duplicate email event eventId={} eventType={} aggregateId={}",
                    eventId,
                    eventType,
                    aggregateId
            );
            return;
        }

        processEmailEvent(payload, eventType, aggregateId);
    }

    private void processEmailEvent(String payload, String eventType, String aggregateId) {
        logger.info(
                "Would send booking email for eventType={} aggregateId={} payload={}",
                eventType,
                aggregateId,
                payload
        );
    }
}
