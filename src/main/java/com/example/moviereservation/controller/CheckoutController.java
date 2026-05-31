package com.example.moviereservation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.example.moviereservation.dto.CheckoutLockResponse;
import com.example.moviereservation.dto.CheckoutLockRequest;
import com.example.moviereservation.dto.CheckoutConfirmResponse;
import com.example.moviereservation.dto.CancelLockRequest;
import com.example.moviereservation.dto.CancelLockResponse;
import com.example.moviereservation.dto.CheckoutConfirmRequest;
import com.example.moviereservation.service.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.moviereservation.dto.CheckoutSessionCreateRequest;
import com.example.moviereservation.dto.CheckoutSessionCreateResponse;
import com.example.moviereservation.service.CheckoutSessionService;

import com.example.moviereservation.dto.CheckoutSessionStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.moviereservation.security.CustomUserPrincipal;
import org.springframework.security.core.Authentication;


@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private CheckoutSessionService checkoutSessionService;
    
    @PostMapping("lock")
    public ResponseEntity<CheckoutLockResponse> lockSeats(
            @RequestBody CheckoutLockRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(checkoutService.lockSeats(request, extractPrincipal(authentication)));
    }

    @PostMapping("confirm")
    // Legacy fake-payment path kept temporarily for development/tests.
    // Real payment flow should use POST /checkout/session and Stripe webhook finalization.
    public ResponseEntity<CheckoutConfirmResponse> confirmCheckout(
            @RequestBody CheckoutConfirmRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(checkoutService.confirmCheckout(request, extractPrincipal(authentication)));
    }

    @PostMapping("cancel")
    public ResponseEntity<CancelLockResponse> cancelLock(
            @RequestBody CancelLockRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(checkoutService.cancelLock(request, extractPrincipal(authentication)));
    }

    private CustomUserPrincipal extractPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    @PostMapping("session")
    // Canonical real-payment entry point: creates a Stripe Checkout Session for active locks.
    public ResponseEntity<CheckoutSessionCreateResponse> createCheckoutSession(
            @RequestBody CheckoutSessionCreateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(checkoutSessionService.createCheckoutSession(request, extractPrincipal(authentication)));
    }

    @GetMapping("session/{checkoutReference}")
    public ResponseEntity<CheckoutSessionStatusResponse> getCheckoutSessionStatus(
            @PathVariable String checkoutReference
    ) {
        return ResponseEntity.ok(
                checkoutSessionService.getCheckoutSessionStatus(
                        checkoutReference,
                        null,
                        null,
                        null
                )
        );
    }
}
