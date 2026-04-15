package com.example.moviereservation.dto;

import java.util.List;

public class CheckoutSessionCreateRequest {

    private Long showtimeId;
    private List<Long> seatIds;
    private String guestEmail;
    private String sessionId;

    public CheckoutSessionCreateRequest() {
    }

    public CheckoutSessionCreateRequest(Long showtimeId, List<Long> seatIds, String guestEmail, String sessionId) {
        this.showtimeId = showtimeId;
        this.seatIds = seatIds;
        this.guestEmail = guestEmail;
        this.sessionId = sessionId;
    }

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
