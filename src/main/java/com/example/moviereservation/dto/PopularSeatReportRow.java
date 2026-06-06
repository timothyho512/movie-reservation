package com.example.moviereservation.dto;

import java.math.BigDecimal;

public record PopularSeatReportRow(
        Long screenId,
        String screenName,
        Long theatreId,
        String theatreName,
        String rowLabel,
        Integer seatNumber,
        String seatType,
        Long bookingCount,
        BigDecimal revenue
) {
}
