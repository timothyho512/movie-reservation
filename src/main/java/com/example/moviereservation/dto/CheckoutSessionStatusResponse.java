package com.example.moviereservation.dto;

import com.example.moviereservation.entity.CheckoutSessionStatus;

public class CheckoutSessionStatusResponse {

    private String checkoutReference;
    private CheckoutSessionStatus status;
    private Long reservationId;
    private String bookingReference;
    private String message;

    public CheckoutSessionStatusResponse() {
    }

    public CheckoutSessionStatusResponse(String checkoutReference,
                                         CheckoutSessionStatus status,
                                         Long reservationId,
                                         String bookingReference,
                                         String message) {
        this.checkoutReference = checkoutReference;
        this.status = status;
        this.reservationId = reservationId;
        this.bookingReference = bookingReference;
        this.message = message;
    }

    public String getCheckoutReference() {
        return checkoutReference;
    }

    public void setCheckoutReference(String checkoutReference) {
        this.checkoutReference = checkoutReference;
    }

    public CheckoutSessionStatus getStatus() {
        return status;
    }

    public void setStatus(CheckoutSessionStatus status) {
        this.status = status;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
