package com.example.moviereservation.controller;

import com.example.moviereservation.service.CheckoutSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout/webhook")
public class StripeWebhookController {

    private final CheckoutSessionService checkoutSessionService;

    public StripeWebhookController(CheckoutSessionService checkoutSessionService) {
        this.checkoutSessionService = checkoutSessionService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        checkoutSessionService.handleStripeWebhook(payload, signatureHeader);
        return ResponseEntity.ok().build();
    }
}
