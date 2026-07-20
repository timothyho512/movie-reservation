package com.example.moviereservation.dto;

import java.time.LocalDateTime;

public record AdminMovieResponse(
        Long id,
        String title,
        String director,
        String posterPath,
        boolean tmdbManaged,
        boolean nowPlaying,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
