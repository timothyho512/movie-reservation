package com.example.moviereservation.dto;

import java.time.LocalDateTime;

public record AdminTheatreResponse(
        Long id,
        String name,
        String address,
        String city,
        String state,
        String country,
        String postalCode,
        String phoneNumber,
        Integer totalScreens,
        Integer totalSeats,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
