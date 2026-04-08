package com.example.moviereservation.dto;

public class CancelReservationRequest {
    private String guestEmail;

    public CancelReservationRequest() {
    }

    public CancelReservationRequest(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }
}
