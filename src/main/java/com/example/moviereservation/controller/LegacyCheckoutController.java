package com.example.moviereservation.controller;

import com.example.moviereservation.dto.CheckoutConfirmRequest;
import com.example.moviereservation.dto.CheckoutConfirmResponse;
import com.example.moviereservation.security.CustomUserPrincipal;
import com.example.moviereservation.service.CheckoutService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
@ConditionalOnProperty(name = "app.legacy-checkout.enabled", havingValue = "true")
public class LegacyCheckoutController {
    private final CheckoutService checkoutService;

    public LegacyCheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("confirm")
    public ResponseEntity<CheckoutConfirmResponse> confirmCheckout(
            @RequestBody CheckoutConfirmRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(checkoutService.confirmCheckout(request, extractPrincipal(authentication)));
    }

    private CustomUserPrincipal extractPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
