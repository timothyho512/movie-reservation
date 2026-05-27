package com.example.moviereservation.controller;

import com.example.moviereservation.dto.AuthUserResponse;
import com.example.moviereservation.dto.UserRequest;
import com.example.moviereservation.dto.UserUpdateRequest;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<AuthUserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers().stream()
                .map(AuthUserResponse::fromUser)
                .toList());
    }

    // Get /api/users/{id} - Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<AuthUserResponse> getUserByid(@PathVariable Long id) {
        return ResponseEntity.ok(AuthUserResponse.fromUser(userService.getUserById(id)));
    }

    // POST /api/users = Create new user
    @PostMapping
    public ResponseEntity<AuthUserResponse> createUser(@RequestBody UserRequest request) {
        User savedUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthUserResponse.fromUser(savedUser));
    }

    // Put /api/movies/{id} - Update user
    @PutMapping("/{id}")
    public ResponseEntity<AuthUserResponse> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(AuthUserResponse.fromUser(updatedUser));
    }

    // DELETE /api/users/{id} - Delete user, (hard delete for testing, soft delete later)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
