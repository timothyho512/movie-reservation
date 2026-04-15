package com.example.moviereservation.dto;

public class StripeCheckoutCompletedEvent {

    private final String stripeCheckoutSessionId;
    private final String stripePaymentIntentId;

    public StripeCheckoutCompletedEvent(String stripeCheckoutSessionId, String stripePaymentIntentId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }
}
