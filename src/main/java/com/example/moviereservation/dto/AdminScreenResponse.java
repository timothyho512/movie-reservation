package com.example.moviereservation.dto;

import com.example.moviereservation.entity.ScreenType;

import java.time.LocalDateTime;

public record AdminScreenResponse(
        Long id,
        String name,
        Long theatreId,
        String theatreName,
        Integer totalSeats,
        ScreenType screenType,
        boolean active,
        Long currentLayoutVersionId,
        Integer currentLayoutVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
