package com.example.moviereservation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeOccupancyReportRow(
        Long showtimeId,
        Long movieId,
        String movieTitle,
        Long theatreId,
        String theatreName,
        Long screenId,
        String screenName,
        LocalDateTime startTime,
        Integer totalSeats,
        Long reservedSeats,
        BigDecimal occupancyRate
) {
}
