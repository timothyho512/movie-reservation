package com.example.moviereservation.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.moviereservation.entity.ReservationStatus;

public class CheckoutConfirmResponse {
    private Long reservationId;
    private String bookingReference;
    private ReservationStatus status;
    private BigDecimal totalPrice;
    private List<Long> seatIds;
    private String message;

    // Constructors
    public CheckoutConfirmResponse() {
    }

    public CheckoutConfirmResponse(Long reservationId, String bookingReference, ReservationStatus status, BigDecimal totalPrice, List<Long> seatIds, String message) {
        this.reservationId = reservationId;
        this.bookingReference = bookingReference;
        // payment status?
        this.status = status;
        this.totalPrice = totalPrice;
        this.seatIds = seatIds;
        this.message = message;
    }

    // Getters and Setters
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

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
