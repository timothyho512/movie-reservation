package com.example.moviereservation.dto;

import java.util.List;

public class CheckoutLockRequest {
    private Long showtimeId;
    private List<Long> seatIds;
    private String guestEmail;  // Optional, can be null for registered users

    // Constructors
    public CheckoutLockRequest() {}

    public CheckoutLockRequest(Long showtimeId, List<Long> seatIds, String guestEmail) {
        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
        this.guestEmail = guestEmail;
    }

    // Getters and Setters
    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }
}
