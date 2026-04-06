package com.example.moviereservation.dto;

public class CancelReservationRequest {
    private Long userId;
    private String guestEmail;

    public CancelReservationRequest() {
    }

    public CancelReservationRequest(Long userId, String guestEmail) {
        this.userId = userId;
        this.guestEmail = guestEmail;
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
}
