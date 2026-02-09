package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.repository.SeatRepository;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatController {
    @Autowired
    private SeatRepository seatRepository;

    // GET /api/seats - Get all seats
    @GetMapping
    public List<Seat> getAllSeats() {
        return seatRepository.findAll();
    }

    // Get /api/seats/{id} - Get seat by ID
    @GetMapping("/{id}")
    public ResponseEntity<Seat> getSeatByid(@PathVariable Long id) {
        return seatRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/seats = Create new seat
    @PostMapping
    public ResponseEntity<Seat> createSeat(@RequestBody Seat seat) {
        Seat savedSeat = seatRepository.save(seat);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSeat);
    }

    // Put /api/seats/{id} - Update seat
    @PutMapping("/{id}")
    public ResponseEntity<Seat> updateSeat(@PathVariable Long id, @RequestBody Seat seatDetails) {
    return seatRepository
        .findById(id)
        .map(
            seat -> {
              seat.setSeatNumber(seatDetails.getSeatNumber());
              Seat updatedSeat = seatRepository.save(seat);
              return ResponseEntity.ok(updatedSeat);
            })
        .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/seats/{id} - Delete seat
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable Long id) {
        if (seatRepository.existsById(id)) {
            seatRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
