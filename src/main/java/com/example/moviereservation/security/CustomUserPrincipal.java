package com.example.moviereservation.security;

import com.example.moviereservation.entity.UserRole;

public class CustomUserPrincipal {
    private final Long userId;
    private final String email;
    private final UserRole role;

    public CustomUserPrincipal(Long userId, String email, UserRole role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }
}
