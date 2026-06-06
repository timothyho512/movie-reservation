package com.example.moviereservation.dto;

import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        Long theatreId,
        String theatreName,
        Long screenId,
        String screenName,
        ScreenType screenType,
        Long layoutVersionId,
        Integer layoutVersion,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal basePrice,
        Integer availableSeats,
        Integer totalSeats,
        ShowtimeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
