package com.example.moviereservation.entity;

public enum PaymentStatus {
    PENDING, //might delete, only if you truly keep an in-between persisted payment state
    PAID,
    REFUNDED,
    FAILED
}
