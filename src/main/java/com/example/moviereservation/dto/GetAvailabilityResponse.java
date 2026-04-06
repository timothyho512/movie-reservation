package com.example.moviereservation.dto;

import java.util.List;

public class GetAvailabilityResponse {
    private Long showtimeId;
    private List<SeatAvailabilityDto> seats;

    // Constructors
    public GetAvailabilityResponse() {}

    public GetAvailabilityResponse(Long showtimeId, List<SeatAvailabilityDto> seats) {
        this.showtimeId = showtimeId;
        this.seats = seats;
    }

    // Getters and Setters
    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public List<SeatAvailabilityDto> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatAvailabilityDto> seats) {
        this.seats = seats;
    }
}
