package com.example.moviereservation.controller;

import com.example.moviereservation.dto.ScreenRequest;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.TheatreRepository;
import com.example.moviereservation.service.ScreenService;
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
    private ScreenService screenService;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private TheatreRepository theatreRepository;

    // GET /api/screens - Get all screens
    @GetMapping
    public ResponseEntity<List<Screen>> getAllScreens() {
        return ResponseEntity.ok(screenService.getAllScreens());
    }

    // Get /api/screens/{id} - Get screen by ID
    @GetMapping("/{id}")
    public ResponseEntity<Screen> getScreenByid(@PathVariable Long id) {
        return ResponseEntity.ok(screenService.getScreenById(id));
    }

    // POST /api/screens = Create new screen
    @PostMapping
    public ResponseEntity<Screen> createScreen(@RequestBody ScreenRequest request) {
        Screen screen = screenService.createScreen(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(screen);

    }

    // Put /api/screens/{id} - Update screen
    @PutMapping("/{id}")
    public ResponseEntity<Screen> updateScreen(@PathVariable Long id, @RequestBody ScreenRequest request) {
        Screen screen = screenService.updateScreen(id, request);
        return ResponseEntity.ok(screen);
    }

    // DELETE /api/screens/{id} - Delete screen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScreen(@PathVariable Long id) {
        screenService.deleteScreen(id);
        return ResponseEntity.noContent().build();
    }
}
