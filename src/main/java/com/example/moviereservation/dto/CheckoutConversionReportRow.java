package com.example.moviereservation.dto;

import java.math.BigDecimal;

public record CheckoutConversionReportRow(
        Long showtimeId,
        Long movieId,
        String movieTitle,
        Long checkoutCount,
        Long paidCheckoutCount,
        Long abandonedCheckoutCount,
        BigDecimal conversionRate,
        BigDecimal abandonedRate
) {
}
