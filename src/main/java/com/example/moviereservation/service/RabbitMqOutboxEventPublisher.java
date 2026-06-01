package com.example.moviereservation.service;

import com.example.moviereservation.config.RabbitMqOutboxConfig;
import com.example.moviereservation.entity.OutboxEvent;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class RabbitMqOutboxEventPublisher implements OutboxEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxEventRoutingKeyMapper routingKeyMapper;

    public RabbitMqOutboxEventPublisher(
            RabbitTemplate rabbitTemplate,
            OutboxEventRoutingKeyMapper routingKeyMapper
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.routingKeyMapper = routingKeyMapper;
    }

    @Override
    public void publish(OutboxEvent event) {
        String routingKey = routingKeyMapper.routingKeyFor(event.getEventType());

        rabbitTemplate.convertAndSend(
                RabbitMqOutboxConfig.EXCHANGE,
                routingKey,
                event.getPayloadJson(),
                message -> {
                    MessageProperties properties = message.getMessageProperties();
                    properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    properties.setHeader("eventId", event.getId());
                    properties.setHeader("eventType", event.getEventType());
                    properties.setHeader("aggregateType", event.getAggregateType());
                    properties.setHeader("aggregateId", event.getAggregateId());
                    return message;
                }
        );
    }
}
