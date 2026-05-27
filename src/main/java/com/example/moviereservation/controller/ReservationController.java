package com.example.moviereservation.controller;

import com.example.moviereservation.dto.ReservationRequest;
import com.example.moviereservation.dto.CancelReservationRequest;
import com.example.moviereservation.dto.CancelReservationResponse;
import com.example.moviereservation.dto.ReservationResponse;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import com.example.moviereservation.security.CustomUserPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    @Autowired
    private ReservationService reservationService;

    // GET /api/reservations - Get all reservations
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getReservations(Authentication authentication) {
        return ResponseEntity.ok(reservationService.getReservationsForUser(extractPrincipal(authentication)));
    }

    @GetMapping("/reference/{reservationReference}")
    public ResponseEntity<ReservationResponse> getGuestReservationByReference(
            @PathVariable String reservationReference,
            @RequestParam String guestEmail
    ) {
        return ResponseEntity.ok(
                reservationService.getGuestReservationByReference(reservationReference, guestEmail)
        );
    }

    // Get /api/reservations/{id} - Get reservation by ID
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(reservationService.getReservationForUser(id, extractPrincipal(authentication)));
    }

    // POST /api/reservations = Create new reservation
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@RequestBody ReservationRequest request) {
        Reservation reservation = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.toReservationResponse(reservation));
    }

    // Put /api/reservations/{id} - Update reservation
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(@PathVariable Long id, @RequestBody ReservationRequest request) {
        Reservation reservation = reservationService.updateReservation(id, request);
        return ResponseEntity.ok(reservationService.toReservationResponse(reservation));
    }

    // DELETE /api/reservations/{id} - Delete reservation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    // Business logic
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancelReservationResponse> cancelReservation(
            @PathVariable Long id,
            @RequestBody(required = false) CancelReservationRequest request,
            Authentication authentication
    ) {
        CancelReservationResponse response =
                reservationService.cancelReservation(
                        id,
                        request != null ? request : new CancelReservationRequest(),
                        extractPrincipal(authentication)
                );
        return ResponseEntity.ok(response);
    }

    private CustomUserPrincipal extractPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
