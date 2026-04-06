package com.example.moviereservation.dto;

import com.example.moviereservation.entity.ReservationStatus;

public class CancelReservationResponse {
    private Long reservationId;
    private ReservationStatus status;
    private String message;

    public CancelReservationResponse() {
    }

    public CancelReservationResponse(Long reservationId, ReservationStatus status, String message) {
        this.reservationId = reservationId;
        this.status = status;
        this.message = message;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
