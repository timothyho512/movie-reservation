package com.example.moviereservation.dto;

import java.time.LocalDateTime;

public record AdminMovieResponse(
        Long id,
        String title,
        String director,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
