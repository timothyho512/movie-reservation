package com.example.moviereservation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.moviereservation.dto.CheckoutLockRequest;
import com.example.moviereservation.dto.CheckoutLockResponse;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.ShowtimeStatus;
import com.example.moviereservation.entity.SeatLock;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.Exception.SeatUnavailableException;
import com.example.moviereservation.dto.CheckoutConfirmRequest;
import com.example.moviereservation.dto.CheckoutConfirmResponse;

import com.example.moviereservation.dto.CancelLockRequest;
import com.example.moviereservation.dto.CancelLockResponse;

import com.example.moviereservation.security.CustomUserPrincipal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class CheckoutService {

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SeatLockRepository seatLockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationService reservationService;

    // the whole thing needs to be transactional
    // validation, loading showtime/seats/user, availability check, lock creation, reservation creation and save
    // all happen inside one service transaction
    @Transactional
    public CheckoutLockResponse lockSeats(CheckoutLockRequest request, CustomUserPrincipal principal) {
        Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());
        // Validate request (showtimeId, seatIds, guest/auth identity)
        validateCheckoutLockRequest(request, effectiveUserId);

        CheckoutContext context = prepareCheckoutContext(request.getShowtimeId(), request.getSeatIds(), effectiveUserId);

        String guestEmail = principal != null ? null : request.getGuestEmail();

        // Check if seats are available for the showtime
        validateSeatsAvailable(context.getShowtime(), context.getSeats());

        // If available, create a temporary lock (e.g., in-memory or Redis) with an expiration time (e.g., 15 minutes)
        try {
            List<SeatLock> seatLocks = createLock(context.getUser(), guestEmail, context.getShowtime(), context.getSeats());

            // Return lock details (sessionId, expiresAt, lockedSeatIds) to the client
            return buildLockResponse(seatLocks);
        } catch (DataIntegrityViolationException e) {
            // If not available, return an error response indicating which seats are unavailable
            throw new SeatUnavailableException("One or more seats are no longer available");
        }
    }

    @Transactional
    public CheckoutConfirmResponse confirmCheckout(CheckoutConfirmRequest request, CustomUserPrincipal principal) {
        Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());
        // Validate request (showtimeId, seatIds, guest/auth identity, sessionId)
        validateCheckoutConfirmRequest(request, effectiveUserId);
  
        CheckoutContext context = prepareCheckoutContext(request.getShowtimeId(), request.getSeatIds(), effectiveUserId);

        String sessionId = principal != null ? null : request.getSessionId();
        String guestEmail = principal != null ? null : request.getGuestEmail();

        // change row status to "processing" to prevent other concurrent requests from using the same lock
        markLocksAsProcessing(context.getShowtime(), context.getSeats(), context.getUser(), sessionId, guestEmail);

        // If valid, create a reservation in the database
        try {
            Reservation reservation = createReservation(context.getUser(), guestEmail, context.getShowtime(), context.getSeats());

            // Update the lock status to "converted to reservation"
            updateLockStatusToConverted(context.getShowtime(), context.getSeats(), context.getUser(), sessionId, guestEmail);

            return buildConfirmResponse(reservation);
        } catch (DataIntegrityViolationException e) {
            // If not available, return an error
            throw new SeatUnavailableException("One or more seats are no longer available");
        }
    }

    @Transactional
    public CancelLockResponse cancelLock(CancelLockRequest request, CustomUserPrincipal principal) {
        Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());
        // Validate request (showtimeId, seatIds, guest/auth identity, sessionId)
        validateCancelLockRequest(request, effectiveUserId);

        CheckoutContext context = prepareCheckoutContext(request.getShowtimeId(), request.getSeatIds(), effectiveUserId);

        String sessionId = principal != null ? null : request.getSessionId();
        String guestEmail = principal != null ? null : request.getGuestEmail();

        cancelLocks(context.getShowtime(), context.getSeats(), context.getUser(), sessionId, guestEmail);

        return buildCancelResponse("Locks cancelled successfully");

    }

    // For logged-in requests, the JWT principal is the only trusted user identity.
    private Long resolveAuthenticatedUserId(CustomUserPrincipal principal, String guestEmail) {
        if (principal == null) {
            return null;
        }

        if (guestEmail != null && !guestEmail.isBlank()) {
            throw new IllegalArgumentException("Guest email must not be provided for authenticated users");
        }

        return principal.getUserId();
    }

    private Showtime loadShowtime(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));
    }

    private List<Seat> loadSeats(List<Long> seatIds) {
        return seatIds.stream()
                .map(seatId -> seatRepository.findById(seatId)
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId)))
                .toList();
    }

    private User loadUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private void validateCommonSeatRequest(Long showtimeId, List<Long> seatIds) {
        if (showtimeId == null) {
            throw new IllegalArgumentException("Showtime ID is required");
        }

        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("At least one seat ID is required");
        }

        if (seatIds.size() != seatIds.stream().distinct().count()) {
            throw new IllegalArgumentException("Duplicate seat IDs are not allowed");
        }
    }

    private void validateUserOrGuestIdentity(Long userId, String guestEmail) {
        boolean hasUserId = userId != null;
        boolean hasGuestEmail = guestEmail != null && !guestEmail.isBlank();

        if (hasUserId == hasGuestEmail) {
            throw new IllegalArgumentException("Exactly one of authenticated user or guestEmail must be provided");
        }

        if (hasGuestEmail) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            if (!guestEmail.matches(emailRegex)) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }
    }

    private void validateSessionIdentity(Long userId, String guestEmail, String sessionId) {
        validateUserOrGuestIdentity(userId, guestEmail);
        boolean hasUserId = userId != null;
        boolean hasGuestEmail = guestEmail != null && !guestEmail.isBlank();
        boolean hasSessionId = sessionId != null && !sessionId.isBlank();

        if (hasGuestEmail && !hasSessionId) {
            throw new IllegalArgumentException("Session ID is required for guest users");
        }

        if (hasUserId && hasSessionId) {
            throw new IllegalArgumentException("Session ID should not be provided for registered users");
        }
    }


    private void validateCheckoutLockRequest(CheckoutLockRequest request, Long effectiveUserId) throws IllegalArgumentException {
        validateCommonSeatRequest(request.getShowtimeId(), request.getSeatIds());
        validateUserOrGuestIdentity(effectiveUserId, request.getGuestEmail());
    }

    private void validateCheckoutConfirmRequest(CheckoutConfirmRequest request, Long effectiveUserId) throws IllegalArgumentException {
        validateCommonSeatRequest(request.getShowtimeId(), request.getSeatIds());
        validateSessionIdentity(effectiveUserId, request.getGuestEmail(), request.getSessionId());
    }

    private void validateCancelLockRequest(CancelLockRequest request, Long effectiveUserId) throws IllegalArgumentException {
        validateCommonSeatRequest(request.getShowtimeId(), request.getSeatIds());
        validateSessionIdentity(effectiveUserId, request.getGuestEmail(), request.getSessionId());
    }


    private void validateSeatsAvailable(Showtime showtime, List<Seat> seats) {

        // checks database row in reservation_seats join table for any of the seatIds and showtimeId that are in the request
        for (Seat seat : seats) {
            boolean isReserved = reservationRepository.existsReservedSeatForShowtime(showtime.getId(), seat.getId());
            if (isReserved) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " is already reserved for this showtime");
            }
            // check if it is locked by another user
            boolean isLocked = seatLockRepository.existsLockedSeatForShowtime(showtime.getId(), seat.getId());
            if (isLocked) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " is currently locked by another user for this showtime");
            }
        }

        // If any seat is not available, throw an exception with details about which seats are unavailable
    }

    private void validateSeatsBelongToShowtime(Showtime showtime, List<Seat> seats) {
        // checks that all seatIds in the request belong to the same showtimeId
        // this is equivalent to checking if they have the same screenId
        for (Seat seat : seats) {
            if (!seat.getScreen().getId().equals(showtime.getScreen().getId())) {
                throw new IllegalArgumentException("Seat " + seat.getId() + " does not belong to the specified showtime");
            }
        }
    }

    private void validateSeatsWithinLimit(List<Seat> seats) {
        // checks that the number of seats requested does not exceed a certain limit (e.g., 10 seats per transaction)
        int seatLimit = 10; // need to put this in config later instead of hardcoding
        if (seats.size() > seatLimit) {
            throw new IllegalArgumentException("Cannot lock more than " + seatLimit + " seats in a single transaction");
        }
    }

    private void validateShowtimeBookable(Showtime showtime) {
        // checks that the showtime is in the future
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new SeatUnavailableException("Cannot lock seats for a showtime that has already started");
        }
        // check showtime status is Upcoming
        if (showtime.getStatus() != ShowtimeStatus.UPCOMING) {
            throw new SeatUnavailableException("Cannot lock seats for a showtime that is not upcoming");
        }
    }

    private void validateCommonCheckoutPreconditions(Showtime showtime, List<Seat> seats) {
        validateShowtimeBookable(showtime);
        validateSeatsWithinLimit(seats);
        validateSeatsBelongToShowtime(showtime, seats);
    }

    private void markLocksAsProcessing(Showtime showtime, List<Seat> seats, User user, String sessionId, String guestEmail) {
        for (Seat seat : seats) {
            int updatedRows;

            if (user != null) {
                updatedRows = seatLockRepository.markLockAsProcessingForUser(showtime.getId(), seat.getId(), user.getId());
            } else {
                updatedRows = seatLockRepository.markLockAsProcessingForSession(showtime.getId(), seat.getId(), sessionId, guestEmail);
            }

            if (updatedRows == 0) {
                // initially message: "Seat 1 is no longer available for confirmation"
                throw new SeatUnavailableException("No valid active lock found for seat " + seat.getId() + " for this confirmation request");
            }
        }
    }

    private void cancelLocks(Showtime showtime, List<Seat> seats, User user, String sessionId, String guestEmail) {
        for (Seat seat : seats) {
            int cancelledRows;
            cancelledRows = seatLockRepository.cancelActiveLock(
                    showtime.getId(),
                    seat.getId(),
                    user != null ? user.getId() : null,
                    sessionId,
                    guestEmail
            );

            if (cancelledRows == 0) {
                throw new SeatUnavailableException("No valid active lock found for seat " + seat.getId() + " for this cancellation request");
            }
        }
    }

    private List<SeatLock> createLock(User user, String guestEmail, Showtime showtime, List<Seat> seats) {
        // Create a lock entry in the database or in-memory store with details about the locked seats, showtime, user/session, and expiration time

        // create sessionId for guest user
        final String sessionId = (guestEmail != null && !guestEmail.isBlank())
        ? UUID.randomUUID().toString()
        : null;

        // save a list of responses for each seat locked, and return the list of locked seatIds in the response
        List<SeatLock> locks = seats.stream()
            .map(seat -> {
                SeatLock seatLock = new SeatLock();
                seatLock.setShowtime(showtime);
                seatLock.setSeat(seat);

                if (user != null) {
                    seatLock.setUser(user);
                } else {
                    seatLock.setSessionId(sessionId);
                    seatLock.setGuestEmail(guestEmail);
                }

                return seatLock;
            })
            .toList();
        
        // if one insert fails, the whole transaction (batch) should rolled back
        return seatLockRepository.saveAll(locks);
    }

    private Reservation createReservation(User user, String guestEmail, Showtime showtime, List<Seat> seats) {
        Reservation reservation = reservationService.createReservation(user, guestEmail, showtime, seats);
        return reservation;
    }

    private void updateLockStatusToConverted(Showtime showtime, List<Seat> seats, User user, String sessionId, String guestEmail) {
        for (Seat seat : seats) {
            int updatedRows;
            updatedRows = seatLockRepository.markLockAsConverted(showtime.getId(), seat.getId(), user != null ? user.getId() : null, sessionId, guestEmail);
            if (updatedRows == 0) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " could not be finalized for reservation");
            }
        }
    }

    // helper function
    private CheckoutLockResponse buildLockResponse(List<SeatLock> seatLocks) {
        String sessionId = seatLocks.getFirst().getSessionId();
        LocalDateTime expiresAt = seatLocks.getFirst().getExpiresAt();
        List<Long> lockedSeatIds = seatLocks.stream()
                .map(lock -> lock.getSeat().getId())
                .toList();
        return new CheckoutLockResponse(sessionId, expiresAt, lockedSeatIds, "Seats locked successfully");
    }

    private CheckoutConfirmResponse buildConfirmResponse(Reservation reservation) {
        return new CheckoutConfirmResponse(reservation.getId(),
                                            reservation.getBookingReference(),
                                            reservation.getStatus(),
                                            reservation.getTotalPrice(),
                                            reservation.getSeats().stream().map(Seat::getId).toList(),
                                            "Reservation confirmed successfully");
    }

    private CancelLockResponse buildCancelResponse(String message) {
        return new CancelLockResponse(message);
    }

    // ===== CheckoutContext =====

    private CheckoutContext prepareCheckoutContext(Long showtimeId, List<Long> seatIds, Long userId) {
        Showtime showtime = loadShowtime(showtimeId);
        List<Seat> seats = loadSeats(seatIds);
        User user = loadUser(userId);

        validateCommonCheckoutPreconditions(showtime, seats);

        return new CheckoutContext(showtime, seats, user);
    }

    private static class CheckoutContext {
        private final Showtime showtime;
        private final List<Seat> seats;
        private final User user;

        private CheckoutContext(Showtime showtime, List<Seat> seats, User user) {
            this.showtime = showtime;
            this.seats = seats;
            this.user = user;
        }

        public Showtime getShowtime() {
            return showtime;
        }

        public List<Seat> getSeats() {
            return seats;
        }

        public User getUser() {
            return user;
        }
    }
}
