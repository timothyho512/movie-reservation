package com.example.moviereservation.dto;

public class StripeCheckoutExpiredEvent {

    private final String stripeCheckoutSessionId;

    public StripeCheckoutExpiredEvent(String stripeCheckoutSessionId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }
}
