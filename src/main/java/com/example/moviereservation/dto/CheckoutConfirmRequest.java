package com.example.moviereservation.dto;

import java.util.List;

public class CheckoutConfirmRequest {
    private Long showtimeId;
    private List<Long> seatIds;
    private Long userId;  // Optional, can be null for guest users
    private String guestEmail;  // Optional, can be null for registered users
    private String sessionId;

    // Constructors
    public CheckoutConfirmRequest() {}

    public CheckoutConfirmRequest(Long showtimeId, List<Long> seatIds, Long userId, String guestEmail, String sessionId) {
        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
        this.userId = userId;
        this.guestEmail = guestEmail;
        this.sessionId = sessionId;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
