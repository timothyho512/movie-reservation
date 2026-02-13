package com.example.moviereservation.controller;

import com.example.moviereservation.dto.UserRequest;
import com.example.moviereservation.dto.UserUpdateRequest;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Get /api/users/{id} - Get user by ID, (fine for testing now, later need to prevent password leak etc)
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserByid(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // POST /api/users = Create new user
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserRequest request) {
        User savedUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // Put /api/movies/{id} - Update user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    // DELETE /api/users/{id} - Delete user, (hard delete for testing, soft delete later)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
