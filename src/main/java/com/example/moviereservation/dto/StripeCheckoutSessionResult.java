package com.example.moviereservation.dto;

public class StripeCheckoutSessionResult {

    private final String checkoutSessionId;
    private final String checkoutUrl;
    private final String paymentIntentId;

    public StripeCheckoutSessionResult(String checkoutSessionId, String checkoutUrl, String paymentIntentId) {
        this.checkoutSessionId = checkoutSessionId;
        this.checkoutUrl = checkoutUrl;
        this.paymentIntentId = paymentIntentId;
    }

    public String getCheckoutSessionId() {
        return checkoutSessionId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }
}
