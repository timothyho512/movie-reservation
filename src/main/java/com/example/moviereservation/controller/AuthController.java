package com.example.moviereservation.controller;

import com.example.moviereservation.dto.AuthResponse;
import com.example.moviereservation.dto.AuthUserResponse;
import com.example.moviereservation.dto.LoginRequest;
import com.example.moviereservation.dto.RegisterRequest;
import com.example.moviereservation.security.CustomUserPrincipal;
import com.example.moviereservation.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        AuthUserResponse response = authService.getCurrentUser(principal.getUserId());
        return ResponseEntity.ok(response);
    }
}
