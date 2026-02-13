package com.example.moviereservation.controller;

import com.example.moviereservation.dto.TheatreRequest;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.TheatreRepository;
import com.example.moviereservation.service.TheatreService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/theatres")
public class TheatreController {
    @Autowired
    private TheatreService theatreService;

    @GetMapping
    public ResponseEntity<List<Theatre>> getAllTheatres() {
        return ResponseEntity.ok(theatreService.getAllTheatres());
    }

    // Get /api/theatres/{id} - Get theatre by ID
    @GetMapping("/{id}")
    public ResponseEntity<Theatre> getTheatreById(@PathVariable Long id) {
        return ResponseEntity.ok(theatreService.getTheatreById(id));
    }

    // POST /api/theatres = Create new theatre
    @PostMapping
    public ResponseEntity<Theatre> createTheatre(@RequestBody TheatreRequest request) {
        Theatre savedTheatre = theatreService.createTheatre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTheatre);
    }

    // Put /api/theatres/{id} - Update theatre
    @PutMapping("/{id}")
    public ResponseEntity<Theatre> updateTheatre(@PathVariable Long id, @RequestBody TheatreRequest request) {
        Theatre updatedTheatre = theatreService.updateTheatre(id, request);
        return ResponseEntity.ok(updatedTheatre);
    }

    // DELETE /api/theatres/{id} - Delete theatre
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTheatre(@PathVariable Long id) {
        theatreService.deleteTheatre(id);
        return ResponseEntity.noContent().build();
    }
}
