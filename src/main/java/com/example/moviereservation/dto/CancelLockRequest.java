package com.example.moviereservation.dto;

import java.util.List;

public class CancelLockRequest {
    private Long showtimeId;
    private List<Long> seatIds;
    private String guestEmail;  // Optional, can be null for registered users
    private String sessionId;

    public CancelLockRequest() {}

    public CancelLockRequest(Long showtimeId, List<Long> seatIds, String guestEmail, String sessionId) {
        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
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
