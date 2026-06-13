package com.example.moviereservation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.moviereservation.entity.CheckoutSessionStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class CheckoutSessionCreateResponse {

    private String checkoutReference;
    private String stripeCheckoutSessionId;
    private String checkoutUrl;
    private CheckoutSessionStatus status;
    private LocalDateTime expiresAt;
    private String message;

    public CheckoutSessionCreateResponse() {
    }

    public CheckoutSessionCreateResponse(String checkoutReference,
                                         String stripeCheckoutSessionId,
                                         String checkoutUrl,
                                         CheckoutSessionStatus status,
                                         LocalDateTime expiresAt,
                                         String message) {
        this.checkoutReference = checkoutReference;
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
        this.checkoutUrl = checkoutUrl;
        this.status = status;
        this.expiresAt = expiresAt;
        this.message = message;
    }

    public String getCheckoutReference() {
        return checkoutReference;
    }

    public void setCheckoutReference(String checkoutReference) {
        this.checkoutReference = checkoutReference;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public CheckoutSessionStatus getStatus() {
        return status;
    }

    public void setStatus(CheckoutSessionStatus status) {
        this.status = status;
    }

    @JsonIgnore
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    @JsonProperty("expiresAt")
    public OffsetDateTime getExpiresAtWithOffset() {
        return expiresAt == null
                ? null
                : expiresAt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
