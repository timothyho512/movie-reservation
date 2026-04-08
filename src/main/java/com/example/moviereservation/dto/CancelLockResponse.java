package com.example.moviereservation.dto;

public class CancelLockResponse {
    private String message;

    public CancelLockResponse() {}

    public CancelLockResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
