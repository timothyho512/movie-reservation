package com.example.moviereservation.controller;

import com.example.moviereservation.dto.ShowtimeRequest;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.service.ShowtimeService;
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
    private ShowtimeService showtimeService;

    // GET /api/showtimes - Get all showtimes
    @GetMapping
    public ResponseEntity<List<Showtime>> getAllShowtimes() {
        return ResponseEntity.ok(showtimeService.getAllShowtimes());
    }

    // Get /api/showtimes/{id} - Get showtime by ID
    @GetMapping("/{id}")
    public ResponseEntity<Showtime> getShowtimeByid(@PathVariable Long id) {
        return ResponseEntity.ok(showtimeService.getShowtimeById(id));
    }

    // POST /api/showtimes = Create new showtime
    @PostMapping
    public ResponseEntity<Showtime> createShowtime(@RequestBody ShowtimeRequest request) {
        Showtime showtime = showtimeService.createShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(showtime);
    }

    // Put /api/showtimes/{id} - Update showtime
    @PutMapping("/{id}")
    public ResponseEntity<Showtime> updateShowtime(@PathVariable Long id, @RequestBody ShowtimeRequest request) {
        Showtime showtime = showtimeService.updateShowtime(id, request);
        return ResponseEntity.ok(showtime);
    }

    // DELETE /api/showtimes/{id} - Delete showtime
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable Long id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }
}
