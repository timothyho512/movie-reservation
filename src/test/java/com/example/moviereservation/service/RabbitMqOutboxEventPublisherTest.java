package com.example.moviereservation.service;

import com.example.moviereservation.config.RabbitMqOutboxConfig;
import com.example.moviereservation.entity.OutboxEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitMqOutboxEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitMqOutboxEventPublisher publisher = new RabbitMqOutboxEventPublisher(
            rabbitTemplate,
            new OutboxEventRoutingKeyMapper()
    );

    @Test
    void publishesOutboxEventPayloadWithRoutingKeyAndHeaders() throws Exception {
        OutboxEvent event = new OutboxEvent(
                OutboxEventService.CHECKOUT_PAYMENT_FINALIZED,
                "CheckoutSession",
                "42",
                "{\"checkoutSessionId\":42}"
        );

        publisher.publish(event);

        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqOutboxConfig.EXCHANGE),
                eq("checkout.payment.finalized"),
                eq("{\"checkoutSessionId\":42}"),
                processorCaptor.capture()
        );

        Message message = new Message("{}".getBytes());
        Message processedMessage = processorCaptor.getValue().postProcessMessage(message);

        assertThat(processedMessage.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(processedMessage.getMessageProperties().getHeaders())
                .containsEntry("eventId", event.getId())
                .containsEntry("eventType", OutboxEventService.CHECKOUT_PAYMENT_FINALIZED)
                .containsEntry("aggregateType", "CheckoutSession")
                .containsEntry("aggregateId", "42");
    }
}
