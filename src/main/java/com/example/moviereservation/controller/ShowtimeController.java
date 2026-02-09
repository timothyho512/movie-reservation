package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.repository.ShowtimeRepository;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {
    @Autowired
    private ShowtimeRepository showtimeRepository;

    // GET /api/showtimes - Get all showtimes
    @GetMapping
    public List<Showtime> getAllShowtimes() {
        return showtimeRepository.findAll();
    }

    // Get /api/showtimes/{id} - Get showtime by ID
    @GetMapping("/{id}")
    public ResponseEntity<Showtime> getShowtimeByid(@PathVariable Long id) {
        return showtimeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/showtimes = Create new showtime
    @PostMapping
    public ResponseEntity<Showtime> createShowtime(@RequestBody Showtime showtime) {
        Showtime savedShowtime = showtimeRepository.save(showtime);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedShowtime);
    }

    // Put /api/showtimes/{id} - Update showtime
    @PutMapping("/{id}")
    public ResponseEntity<Showtime> updateShowtime(@PathVariable Long id, @RequestBody Showtime showtimeDetails) {
    return showtimeRepository
        .findById(id)
        .map(
            showtime -> {
              showtime.setStatus(showtimeDetails.getStatus());
              Showtime updatedShowtime = showtimeRepository.save(showtime);
              return ResponseEntity.ok(updatedShowtime);
            })
        .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/showtimes/{id} - Delete showtime
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable Long id) {
        if (showtimeRepository.existsById(id)) {
            showtimeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
