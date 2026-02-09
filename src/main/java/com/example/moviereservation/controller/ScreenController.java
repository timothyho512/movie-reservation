package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.repository.ScreenRepository;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screens")
public class ScreenController {
    @Autowired
    private ScreenRepository screenRepository;

    // GET /api/screens - Get all screens
    @GetMapping
    public List<Screen> getAllScreens() {
        return screenRepository.findAll();
    }

    // Get /api/screens/{id} - Get screen by ID
    @GetMapping("/{id}")
    public ResponseEntity<Screen> getScreenByid(@PathVariable Long id) {
        return screenRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/screens = Create new screen
    @PostMapping
    public ResponseEntity<Screen> createScreen(@RequestBody Screen screen) {
        Screen savedScreen = screenRepository.save(screen);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedScreen);
    }

    // Put /api/screens/{id} - Update screen
    @PutMapping("/{id}")
    public ResponseEntity<Screen> updateScreen(@PathVariable Long id, @RequestBody Screen screenDetails) {
    return screenRepository
        .findById(id)
        .map(
            screen -> {
              screen.setName(screenDetails.getName());
              Screen updatedScreen = screenRepository.save(screen);
              return ResponseEntity.ok(updatedScreen);
            })
        .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/screens/{id} - Delete screen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScreen(@PathVariable Long id) {
        if (screenRepository.existsById(id)) {
            screenRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
