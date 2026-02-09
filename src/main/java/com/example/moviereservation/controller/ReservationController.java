package com.example.moviereservation.controller;

import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.repository.ReservationRepository;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    @Autowired
    private ReservationRepository reservationRepository;

    // GET /api/reservations - Get all reservations
    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    // Get /api/reservations/{id} - Get reservation by ID
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationByid(@PathVariable Long id) {
        return reservationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/reservations = Create new reservation
    @PostMapping
    public ResponseEntity<Reservation> createReservation(@RequestBody Reservation reservation) {
        Reservation savedReservation = reservationRepository.save(reservation);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedReservation);
    }

    // Put /api/reservations/{id} - Update reservation
    @PutMapping("/{id}")
    public ResponseEntity<Reservation> updateReservation(@PathVariable Long id, @RequestBody Reservation reservationDetails) {
    return reservationRepository
        .findById(id)
        .map(
            reservation -> {
              reservation.setStatus(reservationDetails.getStatus());
              Reservation updatedReservation = reservationRepository.save(reservation);
              return ResponseEntity.ok(updatedReservation);
            })
        .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/reservations/{id} - Delete reservation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        if (reservationRepository.existsById(id)) {
            reservationRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
