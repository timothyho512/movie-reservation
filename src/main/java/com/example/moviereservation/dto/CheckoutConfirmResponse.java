package com.example.moviereservation.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.moviereservation.entity.ReservationStatus;
import com.example.moviereservation.entity.PaymentStatus;

public class CheckoutConfirmResponse {
    private Long reservationId;
    private String bookingReference;
    private ReservationStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal totalPrice;
    private List<Long> seatIds;
    private String message;

    // Constructors
    public CheckoutConfirmResponse() {
    }

    // may need refinement here
    public CheckoutConfirmResponse(Long reservationId, String bookingReference, ReservationStatus status, PaymentStatus paymentStatus, BigDecimal totalPrice, List<Long> seatIds, String message) {
        this.reservationId = reservationId;
        this.bookingReference = bookingReference;
        this.status = status;
        this.paymentStatus = paymentStatus;
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

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
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
