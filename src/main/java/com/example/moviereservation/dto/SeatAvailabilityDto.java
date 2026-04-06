package com.example.moviereservation.dto;

public class SeatAvailabilityDto {
    private Long seatId;
    private boolean available;

    // Constructors
    public SeatAvailabilityDto() {}

    public SeatAvailabilityDto(Long seatId, boolean available) {
        this.seatId = seatId;
        this.available = available;
    }

    // Getters and Setters
    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}