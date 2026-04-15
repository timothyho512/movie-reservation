package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.Exception.SeatUnavailableException;
import com.example.moviereservation.dto.CancelReservationRequest;
import com.example.moviereservation.dto.CancelReservationResponse;
import com.example.moviereservation.dto.ReservationRequest;
import com.example.moviereservation.entity.*;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moviereservation.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {
    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatRepository seatRepository;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    public Reservation createReservation(ReservationRequest request) {
        User user = null;
        if (request.getUserId() != null) {
            // Find user
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
        }

        String guestEmail = request.getGuestEmail();

        validateReservationIdentity(user, guestEmail);

        // Find showtime
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + request.getShowtimeId()));

        // Find all seats
        List<Seat> seats = request.getSeatIds().stream()
                .map(seatId -> seatRepository.findById(seatId)
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId)))
                .collect(Collectors.toList());

        // Calculate total price
        BigDecimal totalPrice = seats.stream()
                .map(Seat::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generate booking reference
        String bookingReference = "BK" + System.currentTimeMillis();

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setGuestEmail(guestEmail);
        reservation.setShowtime(showtime);
        reservation.setSeats(seats);
        reservation.setBookingReference(bookingReference);
        reservation.setNumberOfSeats(seats.size());
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(request.getStatus() != null ? request.getStatus() : ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : PaymentStatus.PAID);
        reservation.setBookingTime(LocalDateTime.now());

        // Update showtime available seats
        showtime.setAvailableSeats(showtime.getAvailableSeats() - seats.size());
        showtimeRepository.save(showtime);

        return reservationRepository.save(reservation);
    }

    public Reservation createReservation(User user, String guestEmail, Showtime showtime, List<Seat> seats) {
        // redundant check, but just to be safe
        validateReservationIdentity(user, guestEmail);

        // Calculate total price
        BigDecimal totalPrice = seats.stream()
                .map(Seat::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generate booking reference
        String bookingReference = "BK" + System.currentTimeMillis();

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setGuestEmail(guestEmail);
        reservation.setShowtime(showtime);
        reservation.setSeats(seats);
        reservation.setBookingReference(bookingReference);
        reservation.setNumberOfSeats(seats.size());
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setBookingTime(LocalDateTime.now());

        // Update showtime available seats
        showtime.setAvailableSeats(showtime.getAvailableSeats() - seats.size());
        showtimeRepository.save(showtime);

        return reservationRepository.save(reservation);
    }

    public Reservation createPaidReservation(User user, String guestEmail, Showtime showtime, List<Seat> seats) {
        validateReservationIdentity(user, guestEmail);

        BigDecimal totalPrice = seats.stream()
                .map(Seat::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String bookingReference = "BK" + System.currentTimeMillis();

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setGuestEmail(guestEmail);
        reservation.setShowtime(showtime);
        reservation.setSeats(seats);
        reservation.setBookingReference(bookingReference);
        reservation.setNumberOfSeats(seats.size());
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setBookingTime(LocalDateTime.now());

        showtime.setAvailableSeats(showtime.getAvailableSeats() - seats.size());
        showtimeRepository.save(showtime);

        return reservationRepository.save(reservation);
    }


    public Reservation updateReservation(Long id, ReservationRequest request) {
        Reservation reservation = getReservationById(id);

        // Update status if provided
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }

        // Update payment status if provided
        if (request.getPaymentStatus() != null) {
            reservation.setPaymentStatus(request.getPaymentStatus());
        }

        return reservationRepository.save(reservation);
    }

    public void deleteReservation(Long id) {
        Reservation reservation = getReservationById(id);

        // Return seats to showtime
        Showtime showtime = reservation.getShowtime();
        showtime.setAvailableSeats(showtime.getAvailableSeats() + reservation.getNumberOfSeats());
        showtimeRepository.save(showtime);

        reservationRepository.delete(reservation);
    }


    // Business logic for canceling reservation
    @Transactional
    public CancelReservationResponse cancelReservation(Long reservationId, CancelReservationRequest request, CustomUserPrincipal principal) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        
        Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());

        validateCancellationIdentity(effectiveUserId, request);
        validateReservationOwnership(reservation, effectiveUserId, request);
        validateReservationCancelable(reservation);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());

        Showtime showtime = reservation.getShowtime();
        showtime.setAvailableSeats(showtime.getAvailableSeats() + reservation.getNumberOfSeats());
        showtimeRepository.save(showtime);

        reservationRepository.save(reservation);

        return new CancelReservationResponse(
                reservation.getId(),
                reservation.getStatus(),
                "Reservation cancelled successfully"
        );
    }

    private Long resolveAuthenticatedUserId(CustomUserPrincipal principal, String guestEmail) {
        if (principal == null) {
            return null;
        }

        if (guestEmail != null && !guestEmail.isBlank()) {
            throw new IllegalArgumentException("Guest email must not be provided for authenticated users");
        }

        return principal.getUserId();
    }

    private void validateCancellationIdentity(Long effectiveUserId, CancelReservationRequest request) {
        boolean hasUserId = effectiveUserId != null;
        boolean hasGuestEmail = request.getGuestEmail() != null && !request.getGuestEmail().isBlank();

        if (hasUserId == hasGuestEmail) {
            throw new IllegalArgumentException("Exactly one of authenticated user or guestEmail must be provided");
        }
    }

    private void validateReservationOwnership(Reservation reservation, Long effectiveUserId, CancelReservationRequest request) {
        if (effectiveUserId != null) {
            if (reservation.getUser() == null || !reservation.getUser().getId().equals(effectiveUserId)) {
                throw new SeatUnavailableException("Reservation does not belong to this user");
            }
            return;
        }

        if (reservation.getGuestEmail() == null || !reservation.getGuestEmail().equals(request.getGuestEmail())) {
            throw new SeatUnavailableException("Reservation does not belong to this guest");
        }
    }

    private void validateReservationCancelable(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new SeatUnavailableException("Reservation is already cancelled");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new SeatUnavailableException("Reservation cannot be cancelled in its current state");
        }

        LocalDateTime cutoff = reservation.getShowtime().getStartTime().minusHours(2);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new SeatUnavailableException("Reservation can no longer be cancelled");
        }
    }


    // Helper function
    private void validateReservationIdentity(User user, String guestEmail) {
    boolean hasUser = user != null;
    boolean hasGuestEmail = guestEmail != null && !guestEmail.isBlank();

    if (hasUser == hasGuestEmail) {
        throw new IllegalArgumentException(
                "Exactly one of user or guestEmail must be provided"
        );
    }
}

}
