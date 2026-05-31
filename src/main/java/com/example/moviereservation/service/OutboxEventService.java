package com.example.moviereservation.service;

import com.example.moviereservation.entity.CheckoutSession;
import com.example.moviereservation.entity.OutboxEvent;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OutboxEventService {

    public static final String RESERVATION_CREATED = "ReservationCreated";
    public static final String RESERVATION_CANCELLED = "ReservationCancelled";
    public static final String CHECKOUT_PAYMENT_FINALIZED = "CheckoutPaymentFinalized";
    public static final String CHECKOUT_SESSION_EXPIRED = "CheckoutSessionExpired";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEvent recordReservationCreated(Reservation reservation) {
        return saveEvent(
                RESERVATION_CREATED,
                "Reservation",
                reservation.getId().toString(),
                reservationPayload(reservation)
        );
    }

    @Transactional
    public OutboxEvent recordReservationCancelled(Reservation reservation) {
        return saveEvent(
                RESERVATION_CANCELLED,
                "Reservation",
                reservation.getId().toString(),
                reservationPayload(reservation)
        );
    }

    @Transactional
    public OutboxEvent recordCheckoutPaymentFinalized(CheckoutSession checkoutSession) {
        return saveEvent(
                CHECKOUT_PAYMENT_FINALIZED,
                "CheckoutSession",
                checkoutSession.getId().toString(),
                checkoutSessionPayload(checkoutSession)
        );
    }

    @Transactional
    public OutboxEvent recordCheckoutSessionExpired(CheckoutSession checkoutSession) {
        return saveEvent(
                CHECKOUT_SESSION_EXPIRED,
                "CheckoutSession",
                checkoutSession.getId().toString(),
                checkoutSessionPayload(checkoutSession)
        );
    }

    private OutboxEvent saveEvent(
            String eventType,
            String aggregateType,
            String aggregateId,
            Map<String, Object> payload
    ) {
        try {
            return outboxEventRepository.save(new OutboxEvent(
                    eventType,
                    aggregateType,
                    aggregateId,
                    objectMapper.writeValueAsString(payload)
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize outbox event payload", e);
        }
    }

    private Map<String, Object> reservationPayload(Reservation reservation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", reservation.getId());
        payload.put("bookingReference", reservation.getBookingReference());
        payload.put("status", reservation.getStatus().name());
        payload.put("paymentStatus", reservation.getPaymentStatus() != null ? reservation.getPaymentStatus().name() : null);
        payload.put("userId", reservation.getUser() != null ? reservation.getUser().getId() : null);
        payload.put("guestEmail", reservation.getGuestEmail());
        payload.put("showtimeId", reservation.getShowtime().getId());
        payload.put("seatIds", seatIds(reservation.getSeats()));
        payload.put("numberOfSeats", reservation.getNumberOfSeats());
        payload.put("totalPrice", reservation.getTotalPrice());
        payload.put("currency", reservation.getCurrency().name());
        payload.put("bookingTime", toString(reservation.getBookingTime()));
        payload.put("cancelledAt", toString(reservation.getCancelledAt()));
        return payload;
    }

    private Map<String, Object> checkoutSessionPayload(CheckoutSession checkoutSession) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("checkoutSessionId", checkoutSession.getId());
        payload.put("checkoutReference", checkoutSession.getCheckoutReference());
        payload.put("stripeCheckoutSessionId", checkoutSession.getStripeCheckoutSessionId());
        payload.put("stripePaymentIntentId", checkoutSession.getStripePaymentIntentId());
        payload.put("status", checkoutSession.getStatus().name());
        payload.put("reservationId", checkoutSession.getReservation() != null ? checkoutSession.getReservation().getId() : null);
        payload.put("userId", checkoutSession.getUser() != null ? checkoutSession.getUser().getId() : null);
        payload.put("guestEmail", checkoutSession.getGuestEmail());
        payload.put("guestSessionId", checkoutSession.getGuestSessionId());
        payload.put("showtimeId", checkoutSession.getShowtime().getId());
        payload.put("itemsSnapshotJson", checkoutSession.getItemsSnapshotJson());
        payload.put("totalAmount", checkoutSession.getTotalAmount());
        payload.put("currency", checkoutSession.getCurrency().name());
        payload.put("expiresAt", toString(checkoutSession.getExpiresAt()));
        payload.put("completedAt", toString(checkoutSession.getCompletedAt()));
        payload.put("cancelledAt", toString(checkoutSession.getCancelledAt()));
        return payload;
    }

    private List<Long> seatIds(List<Seat> seats) {
        return seats.stream()
                .map(Seat::getId)
                .sorted()
                .toList();
    }

    private String toString(LocalDateTime timestamp) {
        return timestamp != null ? timestamp.toString() : null;
    }
}
