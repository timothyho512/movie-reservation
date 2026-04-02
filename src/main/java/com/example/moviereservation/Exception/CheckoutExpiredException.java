package com.example.moviereservation.Exception;

public class CheckoutExpiredException extends RuntimeException {
    public CheckoutExpiredException(String message) {
        super(message);
    }
}
