package com.example.moviereservation.controller;

import com.example.moviereservation.dto.SeatRequest;
import com.example.moviereservation.dto.SeatResponse;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.service.SeatService;
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
    public ResponseEntity<List<SeatResponse>> getAllSeats() {
        return ResponseEntity.ok(seatService.getSeatResponses());
    }

    // Get /api/seats/{id} - Get seat by ID
    @GetMapping("/{id}")
    public ResponseEntity<SeatResponse> getSeatByid(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.getSeatResponse(id));
    }

    // POST /api/seats = Create new seat
    @PostMapping
    public ResponseEntity<SeatResponse> createSeat(@RequestBody SeatRequest request) {
        Seat seat = seatService.createSeat(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.toSeatResponse(seat));
    }

    // Put /api/seats/{id} - Update seat
    @PutMapping("/{id}")
    public ResponseEntity<SeatResponse> updateSeat(@PathVariable Long id, @RequestBody SeatRequest request) {
        Seat seat = seatService.updateSeat(id, request);
        return ResponseEntity.ok(seatService.toSeatResponse(seat));
    }

    // DELETE /api/seats/{id} - Delete seat
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable Long id) {
        seatService.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }
}
