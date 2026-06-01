package com.example.moviereservation.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true")
public class RabbitMqOutboxConfig {

    public static final String EXCHANGE = "movie-reservation.events";
    public static final String EMAIL_QUEUE = "movie-reservation.email";
    public static final String REPORTING_QUEUE = "movie-reservation.reporting";
    public static final String AUDIT_QUEUE = "movie-reservation.audit";

    @Bean
    public TopicExchange movieReservationEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public Queue reportingQueue() {
        return new Queue(REPORTING_QUEUE, true);
    }

    @Bean
    public Queue auditQueue() {
        return new Queue(AUDIT_QUEUE, true);
    }

    @Bean
    public Binding emailReservationCreatedBinding(
            @Qualifier("emailQueue") Queue emailQueue,
            TopicExchange movieReservationEventsExchange
    ) {
        return BindingBuilder.bind(emailQueue).to(movieReservationEventsExchange).with("reservation.created");
    }

    @Bean
    public Binding emailReservationCancelledBinding(
            @Qualifier("emailQueue") Queue emailQueue,
            TopicExchange movieReservationEventsExchange
    ) {
        return BindingBuilder.bind(emailQueue).to(movieReservationEventsExchange).with("reservation.cancelled");
    }

    @Bean
    public Binding emailCheckoutPaymentFinalizedBinding(
            @Qualifier("emailQueue") Queue emailQueue,
            TopicExchange movieReservationEventsExchange
    ) {
        return BindingBuilder.bind(emailQueue).to(movieReservationEventsExchange).with("checkout.payment.finalized");
    }

    @Bean
    public Binding reportingBinding(
            @Qualifier("reportingQueue") Queue reportingQueue,
            TopicExchange movieReservationEventsExchange
    ) {
        return BindingBuilder.bind(reportingQueue).to(movieReservationEventsExchange).with("#");
    }

    @Bean
    public Binding auditBinding(
            @Qualifier("auditQueue") Queue auditQueue,
            TopicExchange movieReservationEventsExchange
    ) {
        return BindingBuilder.bind(auditQueue).to(movieReservationEventsExchange).with("#");
    }
}
