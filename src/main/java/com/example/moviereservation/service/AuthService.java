package com.example.moviereservation.service;

import com.example.moviereservation.Exception.AuthenticationFailedException;
import com.example.moviereservation.Exception.DuplicateEmailException;
import com.example.moviereservation.dto.AuthResponse;
import com.example.moviereservation.dto.AuthUserResponse;
import com.example.moviereservation.dto.LoginRequest;
import com.example.moviereservation.dto.RegisterRequest;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.entity.UserRole;
import com.example.moviereservation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("Email is already registered");
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(normalizeOptional(request.getPhoneNumber()));
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now()); // use Prepersist instead?
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getRole().name()
        );

        // JWT will be added in the next patch.
        return new AuthResponse(token, AuthUserResponse.fromUser(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        validateLoginRequest(request);

        String normalizedEmail = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        String token = jwtService.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );

        // JWT will be added in the next patch.
        return new AuthResponse(token, AuthUserResponse.fromUser(user));
    }

    public AuthUserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("User account is inactive");
        }

        return AuthUserResponse.fromUser(user);
    }


    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Register request is required");
        }

        if (isBlank(request.getFirstName())) {
            throw new IllegalArgumentException("First name is required");
        }

        if (isBlank(request.getLastName())) {
            throw new IllegalArgumentException("Last name is required");
        }

        if (isBlank(request.getEmail())) {
            throw new IllegalArgumentException("Email is required");
        }

        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (isBlank(request.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }

        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request is required");
        }

        if (isBlank(request.getEmail())) {
            throw new IllegalArgumentException("Email is required");
        }

        if (isBlank(request.getPassword())) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }
}
