package com.example.moviereservation.entity;

public enum CheckoutSessionStatus {
    PENDING_PAYMENT,
    PAID,
    FAILED,
    CANCELLED,
    EXPIRED,
    FINALIZED,
    REFUND_PENDING,
    REFUNDED
}
