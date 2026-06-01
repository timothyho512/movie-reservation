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

    @RabbitListener(queues = RabbitMqOutboxConfig.EMAIL_QUEUE)
    public void handleEmailEvent(
            String payload,
            @Header(name = "eventType", required = false) String eventType,
            @Header(name = "aggregateId", required = false) String aggregateId
    ) {
        logger.info(
                "Would send booking email for eventType={} aggregateId={} payload={}",
                eventType,
                aggregateId,
                payload
        );
    }
}
