package com.example.moviereservation.service;

import org.springframework.stereotype.Component;

@Component
public class OutboxEventRoutingKeyMapper {

    public String routingKeyFor(String eventType) {
        return switch (eventType) {
            case OutboxEventService.RESERVATION_CREATED -> "reservation.created";
            case OutboxEventService.RESERVATION_CANCELLED -> "reservation.cancelled";
            case OutboxEventService.CHECKOUT_PAYMENT_FINALIZED -> "checkout.payment.finalized";
            case OutboxEventService.CHECKOUT_SESSION_EXPIRED -> "checkout.session.expired";
            default -> throw new IllegalArgumentException("Unsupported outbox event type: " + eventType);
        };
    }
}
