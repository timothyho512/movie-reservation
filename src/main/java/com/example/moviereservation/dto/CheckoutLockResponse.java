package com.example.moviereservation.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CheckoutLockResponse {
    private String sessionId;
    private LocalDateTime expiresAt;
    private List<Long> lockedSeatIds;
    private String message;

    // Constructors
    public CheckoutLockResponse() {
    }

    public CheckoutLockResponse(String sessionId, LocalDateTime expiresAt, List<Long> lockedSeatIds, String message) {
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
        this.lockedSeatIds = lockedSeatIds;
        this.message = message;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<Long> getLockedSeatIds() {
        return lockedSeatIds;
    }

    public void setLockedSeatIds(List<Long> lockedSeatIds) {
        this.lockedSeatIds = lockedSeatIds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;

    }
}
