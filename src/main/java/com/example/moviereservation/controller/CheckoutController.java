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

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    @Autowired
    private CheckoutService checkoutService;
    
    @PostMapping("lock")
    public ResponseEntity<CheckoutLockResponse> lockSeats(@RequestBody CheckoutLockRequest request) {
        return ResponseEntity.ok(checkoutService.lockSeats(request));
    }

    @PostMapping("confirm")
    public ResponseEntity<CheckoutConfirmResponse> confirmCheckout(@RequestBody CheckoutConfirmRequest request) {
        return ResponseEntity.ok(checkoutService.confirmCheckout(request));
    }

    @PostMapping("cancel")
    public ResponseEntity<CancelLockResponse> cancelLock(@RequestBody CancelLockRequest request) {
        return ResponseEntity.ok(checkoutService.cancelLock(request));
    }
}
