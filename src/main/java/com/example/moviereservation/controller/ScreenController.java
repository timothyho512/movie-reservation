package com.example.moviereservation.controller;

import com.example.moviereservation.dto.ScreenRequest;
import com.example.moviereservation.dto.ScreenResponse;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.service.ScreenService;
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

    // GET /api/screens - Get all screens
    @GetMapping
    public ResponseEntity<List<ScreenResponse>> getAllScreens() {
        return ResponseEntity.ok(screenService.getScreenResponses());
    }

    // Get /api/screens/{id} - Get screen by ID
    @GetMapping("/{id}")
    public ResponseEntity<ScreenResponse> getScreenByid(@PathVariable Long id) {
        return ResponseEntity.ok(screenService.getScreenResponse(id));
    }

    // POST /api/screens = Create new screen
    @PostMapping
    public ResponseEntity<ScreenResponse> createScreen(@RequestBody ScreenRequest request) {
        Screen screen = screenService.createScreen(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(screenService.toScreenResponse(screen));

    }

    // Put /api/screens/{id} - Update screen
    @PutMapping("/{id}")
    public ResponseEntity<ScreenResponse> updateScreen(@PathVariable Long id, @RequestBody ScreenRequest request) {
        Screen screen = screenService.updateScreen(id, request);
        return ResponseEntity.ok(screenService.toScreenResponse(screen));
    }

    // DELETE /api/screens/{id} - Delete screen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScreen(@PathVariable Long id) {
        screenService.deleteScreen(id);
        return ResponseEntity.noContent().build();
    }
}
