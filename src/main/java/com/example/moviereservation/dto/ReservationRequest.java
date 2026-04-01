package com.example.moviereservation.dto;

import com.example.moviereservation.entity.PaymentStatus;
import com.example.moviereservation.entity.ReservationStatus;

import java.util.List;

public class ReservationRequest {
    private Long userId;
    private String guestEmail;
    private Long showtimeId;
    private List<Long> seatIds;
    private ReservationStatus status;
    private PaymentStatus paymentStatus;

    // Constructors
    public ReservationRequest() {
    }

    public ReservationRequest(Long userId, String guestEmail, Long showtimeId, List<Long> seatIds) {
        this.userId = userId;
        this.guestEmail = guestEmail;
        this.showtimeId = showtimeId;
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
}
