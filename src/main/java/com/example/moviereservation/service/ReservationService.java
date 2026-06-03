package com.example.moviereservation.service;

import com.example.moviereservation.Exception.AuthenticationFailedException;
import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.Exception.SeatUnavailableException;
import com.example.moviereservation.dto.CancelReservationRequest;
import com.example.moviereservation.dto.CancelReservationResponse;
import com.example.moviereservation.dto.ReservationRequest;
import com.example.moviereservation.dto.ReservationResponse;
import com.example.moviereservation.entity.*;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.service.OutboxEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moviereservation.security.CustomUserPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
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

    @Autowired
    private RedisSeatMapCacheService redisSeatMapCacheService;

    @Autowired
    private OutboxEventService outboxEventService;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsForUser(CustomUserPrincipal principal) {
        Long userId = requireAuthenticatedUser(principal);

        return reservationRepository.findAllByUserIdOrderByBookingTimeDesc(userId).stream()
                .map(this::toReservationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationForUser(Long reservationId, CustomUserPrincipal principal) {
        Long userId = requireAuthenticatedUser(principal);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));

        if (reservation.getUser() == null || !reservation.getUser().getId().equals(userId)) {
            throw new SeatUnavailableException("Reservation does not belong to this user");
        }

        return toReservationResponse(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getGuestReservationByReference(String reservationReference, String guestEmail) {
        if (guestEmail == null || guestEmail.isBlank()) {
            throw new IllegalArgumentException("Guest email is required");
        }

        Reservation reservation = reservationRepository.findByBookingReference(reservationReference)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with reference: " + reservationReference));

        if (reservation.getGuestEmail() == null
                || !reservation.getGuestEmail().trim().equalsIgnoreCase(guestEmail.trim())) {
            throw new SeatUnavailableException("Reservation does not belong to this guest");
        }

        return toReservationResponse(reservation);
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
        reservation.setCurrency(CurrencyCode.GBP);  // Default currency
        reservation.setStatus(request.getStatus() != null ? request.getStatus() : ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : PaymentStatus.PAID);
        reservation.setBookingTime(LocalDateTime.now());

        // Update showtime available seats
        showtime.setAvailableSeats(showtime.getAvailableSeats() - seats.size());
        showtimeRepository.save(showtime);

        Reservation savedReservation = reservationRepository.save(reservation);
        outboxEventService.recordReservationCreated(savedReservation);
        redisSeatMapCacheService.evict(showtime.getId());
        return savedReservation;
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
        reservation.setCurrency(CurrencyCode.GBP);  // Default currency
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setBookingTime(LocalDateTime.now());

        // Update showtime available seats
        showtime.setAvailableSeats(showtime.getAvailableSeats() - seats.size());
        showtimeRepository.save(showtime);

        Reservation savedReservation = reservationRepository.save(reservation);
        outboxEventService.recordReservationCreated(savedReservation);
        redisSeatMapCacheService.evict(showtime.getId());
        return savedReservation;
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
        reservation.setCurrency(CurrencyCode.GBP);  // Default currency
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setBookingTime(LocalDateTime.now());

        showtime.setAvailableSeats(showtime.getAvailableSeats() - seats.size());
        showtimeRepository.save(showtime);

        Reservation savedReservation = reservationRepository.save(reservation);
        outboxEventService.recordReservationCreated(savedReservation);
        redisSeatMapCacheService.evict(showtime.getId());
        return savedReservation;
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
        redisSeatMapCacheService.evict(showtime.getId());
    }


    // Business logic for canceling reservation
    @Transactional
    public CancelReservationResponse cancelReservation(Long reservationId, CancelReservationRequest request, CustomUserPrincipal principal) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        
        Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());

        validateCancellationIdentity(effectiveUserId, request);
        validateReservationOwnership(reservation, effectiveUserId, request, principal);
        validateReservationCancelable(reservation);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());

        Showtime showtime = reservation.getShowtime();
        showtime.setAvailableSeats(showtime.getAvailableSeats() + reservation.getNumberOfSeats());
        showtimeRepository.save(showtime);

        reservationRepository.save(reservation);
        outboxEventService.recordReservationCancelled(reservation);
        redisSeatMapCacheService.evict(showtime.getId());

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

    private void validateReservationOwnership(
            Reservation reservation,
            Long effectiveUserId,
            CancelReservationRequest request,
            CustomUserPrincipal principal
    ) {
        if (isAdminOrManager(principal)) {
            return;
        }

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

    private boolean isAdminOrManager(CustomUserPrincipal principal) {
        return principal != null
                && (principal.getRole() == UserRole.ADMIN || principal.getRole() == UserRole.MANAGER);
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

    private Long requireAuthenticatedUser(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new AuthenticationFailedException("Authentication is required to access this resource");
        }

        return principal.getUserId();
    }

    public ReservationResponse toReservationResponse(Reservation reservation) {
        Showtime showtime = reservation.getShowtime();
        Movie movie = showtime.getMovie();
        Screen screen = showtime.getScreen();

        List<ReservationResponse.SeatSummary> seats = reservation.getSeats().stream()
                .sorted(Comparator
                        .comparing(Seat::getRowLabel)
                        .thenComparing(Seat::getSeatNumber)
                        .thenComparing(Seat::getId))
                .map(seat -> new ReservationResponse.SeatSummary(
                        seat.getId(),
                        seat.getRowLabel(),
                        seat.getSeatNumber(),
                        seat.getSeatType()
                ))
                .toList();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getBookingReference(),
                reservation.getStatus(),
                reservation.getPaymentStatus(),
                new ReservationResponse.ShowtimeSummary(
                        showtime.getId(),
                        showtime.getStartTime(),
                        showtime.getEndTime()
                ),
                new ReservationResponse.MovieSummary(
                        movie.getId(),
                        movie.getTitle(),
                        movie.getDirector()
                ),
                new ReservationResponse.ScreenSummary(
                        screen.getId(),
                        screen.getName(),
                        screen.getScreenType()
                ),
                seats,
                reservation.getTotalPrice(),
                reservation.getCurrency() != null ? reservation.getCurrency() : CurrencyCode.GBP,
                reservation.getBookingTime()
        );
    }

}
