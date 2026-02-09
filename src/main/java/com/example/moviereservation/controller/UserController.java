package com.example.moviereservation.controller;

import com.example.moviereservation.dto.UserUpdateRequest;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.repository.UserRepository;
import org.apache.coyote.Response;
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
    private UserRepository userRepository;

    // GET /api/users - Get all users, (not ok for production, fine for now), later need filtered version
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get /api/users/{id} - Get user by ID, (fine for testing now, later need to prevent password leak etc)
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserByid(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/users = Create new user
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // Put /api/movies/{id} - Update user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
    return userRepository
        .findById(id)
        .map(
            user -> {
                if (request.getFirstName() != null) {
                    user.setFirstName(request.getFirstName());
                }
                if (request.getLastName() != null) {
                    user.setLastName(request.getLastName());
                }
                if (request.getPhoneNumber() != null) {
                    user.setPhoneNumber(request.getPhoneNumber());
                }
                user.setUpdatedAt(LocalDateTime.now());
                // DON'T allow password update here - need separate endpoint
              User updatedUser = userRepository.save(user);
              return ResponseEntity.ok(updatedUser);
            })
        .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/users/{id} - Delete user, (hard delete for testing, soft delete later)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
