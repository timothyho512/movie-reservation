package com.example.moviereservation.dto;

import java.math.BigDecimal;

public record MovieRevenueReportRow(
        Long movieId,
        String movieTitle,
        Long reservationCount,
        Long ticketsSold,
        BigDecimal revenue
) {
}
