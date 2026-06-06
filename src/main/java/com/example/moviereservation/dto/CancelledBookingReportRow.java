package com.example.moviereservation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CancelledBookingReportRow(
        Long reservationId,
        String bookingReference,
        Long movieId,
        String movieTitle,
        Long showtimeId,
        LocalDateTime showtimeStartTime,
        LocalDateTime cancelledAt,
        Integer numberOfSeats,
        BigDecimal totalPrice,
        String paymentStatus
) {
}
