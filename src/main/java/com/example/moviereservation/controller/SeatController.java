package com.example.moviereservation.controller;

import com.example.moviereservation.dto.SeatRequest;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.service.SeatService;
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
    private SeatService seatService;

    // GET /api/seats - Get all seats
    @GetMapping
    public ResponseEntity<List<Seat>> getAllSeats() {
        return ResponseEntity.ok(seatService.getAllSeats());
    }

    // Get /api/seats/{id} - Get seat by ID
    @GetMapping("/{id}")
    public ResponseEntity<Seat> getSeatByid(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    // POST /api/seats = Create new seat
    @PostMapping
    public ResponseEntity<Seat> createSeat(@RequestBody SeatRequest request) {
        Seat seat = seatService.createSeat(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(seat);
    }

    // Put /api/seats/{id} - Update seat
    @PutMapping("/{id}")
    public ResponseEntity<Seat> updateSeat(@PathVariable Long id, @RequestBody SeatRequest request) {
        Seat seat = seatService.updateSeat(id, request);
        return ResponseEntity.ok(seat);
    }

    // DELETE /api/seats/{id} - Delete seat
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable Long id) {
        seatService.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }
}
