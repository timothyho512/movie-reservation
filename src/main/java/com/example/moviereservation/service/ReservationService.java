package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.ReservationRequest;
import com.example.moviereservation.entity.*;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        // Find user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

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
        reservation.setShowtime(showtime);
        reservation.setSeats(seats);
        reservation.setBookingReference(bookingReference);
        reservation.setNumberOfSeats(seats.size());
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(request.getStatus() != null ? request.getStatus() : ReservationStatus.PENDING);
        reservation.setPaymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : PaymentStatus.PENDING);
        reservation.setBookingTime(LocalDateTime.now());

        // Update showtime available seats
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
}
