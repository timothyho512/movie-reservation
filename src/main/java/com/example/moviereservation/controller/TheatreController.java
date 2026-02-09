package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.TheatreRepository;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
public class TheatreController {
    @Autowired
    private TheatreRepository theatreRepository;

    // GET /api/theatres - Get all theatres
    @GetMapping
    public List<Theatre> getAllTheatres() {
        return theatreRepository.findAll();
    }

    // Get /api/theatres/{id} - Get theatre by ID
    @GetMapping("/{id}")
    public ResponseEntity<Theatre> getTheatreById(@PathVariable Long id) {
        return theatreRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/theatres = Create new theatre
    @PostMapping
    public ResponseEntity<Theatre> createTheatre(@RequestBody Theatre theatre) {
        Theatre savedTheatre = theatreRepository.save(theatre);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTheatre);
    }

    // Put /api/theatres/{id} - Update theatre
    @PutMapping("/{id}")
    public ResponseEntity<Theatre> updateTheatre(@PathVariable Long id, @RequestBody Theatre theatreDetails) {
    return theatreRepository
        .findById(id)
        .map(
            theatre -> {
              theatre.setName(theatreDetails.getName());
              Theatre updatedTheatre = theatreRepository.save(theatre);
              return ResponseEntity.ok(updatedTheatre);
            })
        .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/theatres/{id} - Delete theatre
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTheatre(@PathVariable Long id) {
        if (theatreRepository.existsById(id)) {
            theatreRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
