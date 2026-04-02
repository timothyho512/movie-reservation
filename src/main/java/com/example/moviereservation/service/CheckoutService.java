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
import com.example.moviereservation.entity.LockStatus;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.Exception.SeatUnavailableException;
import com.example.moviereservation.dto.CheckoutConfirmRequest;
import com.example.moviereservation.dto.CheckoutConfirmResponse;

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
    public CheckoutLockResponse lockSeats(CheckoutLockRequest request) {
        // Validate request (showtimeId, seatIds, userId/guestEmail)
        validateCheckoutLockRequest(request);

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + request.getShowtimeId()));
        
        // Find all seats
        List<Seat> seats = request.getSeatIds().stream()
                .map(seatId -> seatRepository.findById(seatId)
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId)))
                .toList();

        User user = resolveUser(request.getUserId());
        
        // check if showtime is bookable (not started, not cancelled)
        isShowtimeBookable(showtime);
        // check if the input seats is valid and not exceeding the limit
        isSeatsExceedLimit(seats);
        // check if the seats belong to the showtime
        isSeatsBelongToShowtime(showtime, seats);

        String guestEmail = request.getGuestEmail();
        validateLockIdentity(user, guestEmail);

        // Check if seats are available for the showtime
        isSeatsAvailable(showtime, seats);

        // If available, create a temporary lock (e.g., in-memory or Redis) with an expiration time (e.g., 15 minutes)
        try {
            List<SeatLock> seatLocks = createLock(user, guestEmail, showtime, seats);

            // Return lock details (sessionId, expiresAt, lockedSeatIds) to the client
            return buildLockResponse(seatLocks);
        } catch (DataIntegrityViolationException e) {
            // If not available, return an error response indicating which seats are unavailable
            throw new SeatUnavailableException("One or more seats are no longer available");
        }
    }

    @Transactional
    public CheckoutConfirmResponse confirmCheckout(CheckoutConfirmRequest request) {
        // Validate request (showtimeId, seatIds, userId/guestEmail, sessionId)
        validateCheckoutConfirmRequest(request);
        // Verify the lock is still valid and matches the requested seats
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + request.getShowtimeId()));
        
        // Find all seats
        List<Seat> seats = request.getSeatIds().stream()
                .map(seatId -> seatRepository.findById(seatId)
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId)))
                .toList();

        User user = resolveUser(request.getUserId());

        String sessionId = request.getSessionId();
        
        // usual safety checks before confirming reservation
        // check if showtime is bookable (not started, not cancelled)
        isShowtimeBookable(showtime);
        // check if the input seats is valid and not exceeding the limit
        isSeatsExceedLimit(seats);
        // check if the seats belong to the showtime
        isSeatsBelongToShowtime(showtime, seats);

        String guestEmail = request.getGuestEmail();
        validateLockIdentity(user, guestEmail, sessionId);
        // change row status to "processing" to prevent other concurrent requests from using the same lock
        markLocksAsProcessing(showtime, seats, user, sessionId);

        // If valid, create a reservation in the database
        try {
            Reservation reservation = reserveSeats(user, guestEmail, showtime, seats);

            // Update the lock status to "converted to reservation"
            updateLockStatusToConverted(showtime, seats, user, sessionId);

            // Update showtime's available seats
            showtime.setAvailableSeats(showtime.getAvailableSeats() - seats.size());
            showtimeRepository.save(showtime);

            return buildConfirmResponse(reservation);
        } catch (DataIntegrityViolationException e) {
            // If not available, return an error
            throw new SeatUnavailableException("One or more seats are no longer available");
        }
    }


    // Helper method to validate lock request
    private void validateCheckoutLockRequest(CheckoutLockRequest request) throws IllegalArgumentException {
        // Validate showtimeId, seatIds, and either userId or guestEmail

        if (request.getShowtimeId() == null) {
            throw new IllegalArgumentException("Showtime ID is required");
        }
        
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new IllegalArgumentException("At least one seat ID is required");
        }

        // seatIds should not contain duplicates
        if (request.getSeatIds().size() != request.getSeatIds().stream().distinct().count()) {
            throw new IllegalArgumentException("Duplicate seat IDs are not allowed");
        }

        // // seatIds should be positive numbers
        // if (request.getSeatIds().stream().anyMatch(seatId -> seatId <= 0)) {
        //     throw new IllegalArgumentException("Seat IDs must be positive numbers");
        // }

        // seats should be for the same showtime - this can be checked later when we fetch the seat entities

        if (request.getUserId() == null && (request.getGuestEmail() == null || request.getGuestEmail().isEmpty())) {
            throw new IllegalArgumentException("Either user ID or guest email is required");
        }

        // if email is provided, it should be in a valid format
        if (request.getGuestEmail() != null && !request.getGuestEmail().isEmpty()) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            if (!request.getGuestEmail().matches(emailRegex)) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }
    }

    private void validateCheckoutConfirmRequest(CheckoutConfirmRequest request) throws IllegalArgumentException {
        // Validate showtimeId, seatIds, userId/guestEmail, and sessionId
        if (request.getShowtimeId() == null) {
            throw new IllegalArgumentException("Showtime ID is required");
        }
        
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new IllegalArgumentException("At least one seat ID is required");
        }

        // seatIds should not contain duplicates
        if (request.getSeatIds().size() != request.getSeatIds().stream().distinct().count()) {
            throw new IllegalArgumentException("Duplicate seat IDs are not allowed");
        }

        // // seatIds should be positive numbers
        // if (request.getSeatIds().stream().anyMatch(seatId -> seatId <= 0)) {
        //     throw new IllegalArgumentException("Seat IDs must be positive numbers");
        // }

        // seats should be for the same showtime - this can be checked later when we fetch the seat entities

        if (request.getUserId() == null && (request.getGuestEmail() == null || request.getGuestEmail().isEmpty())) {
            throw new IllegalArgumentException("Either user ID or guest email is required");
        }

        // if email is provided, it should be in a valid format
        if (request.getGuestEmail() != null && !request.getGuestEmail().isEmpty()) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            if (!request.getGuestEmail().matches(emailRegex)) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }

        // cehck that sessionId is provided if guestEmail is provided, and not provided if userId is provided
        if (request.getGuestEmail() != null && !request.getGuestEmail().isEmpty() && (request.getSessionId() == null || request.getSessionId().isEmpty())) {
            throw new IllegalArgumentException("Session ID is required for guest users");
        }

        if (request.getUserId() != null && (request.getSessionId() != null && !request.getSessionId().isEmpty())) {
            throw new IllegalArgumentException("Session ID should not be provided for registered users");
        }
    }


    private void isSeatsAvailable(Showtime showtime, List<Seat> seats) {

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

    private void isSeatsBelongToShowtime(Showtime showtime, List<Seat> seats) {
        // checks that all seatIds in the request belong to the same showtimeId
        // this is equivalent to checking if they have the same screenId
        for (Seat seat : seats) {
            if (!seat.getScreen().getId().equals(showtime.getScreen().getId())) {
                throw new IllegalArgumentException("Seat " + seat.getId() + " does not belong to the specified showtime");
            }
        }
    }

    private void isSeatsExceedLimit(List<Seat> seats) {
        // checks that the number of seats requested does not exceed a certain limit (e.g., 10 seats per transaction)
        int seatLimit = 10; // need to put this in config later instead of hardcoding
        if (seats.size() > seatLimit) {
            throw new IllegalArgumentException("Cannot lock more than " + seatLimit + " seats in a single transaction");
        }
    }

    private void isShowtimeBookable(Showtime showtime) {
        // checks that the showtime is in the future
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot lock seats for a showtime that has already started");
        }
        // check showtime status is Upcoming
        if (showtime.getStatus() != ShowtimeStatus.UPCOMING) {
            throw new IllegalArgumentException("Cannot lock seats for a showtime that is not upcoming");
        }
    }

    private void markLocksAsProcessing(Showtime showtime, List<Seat> seats, User user, String sessionId) {
        for (Seat seat : seats) {
            int updatedRows;

            if (user != null) {
                updatedRows = seatLockRepository.markLockAsProcessingForUser(showtime.getId(), seat.getId(), user.getId());
            } else {
                updatedRows = seatLockRepository.markLockAsProcessingForSession(showtime.getId(), seat.getId(), sessionId);
            }

            if (updatedRows == 0) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " is no longer available for confirmation");
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
                }

                return seatLock;
            })
            .toList();
        
        // if one insert fails, the whole transaction (batch) should rolled back
        return seatLockRepository.saveAll(locks);
    }

    // helper function
    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Reservation reserveSeats(User user, String guestEmail, Showtime showtime, List<Seat> seats) {
        Reservation reservation = reservationService.createReservation(user, guestEmail, showtime, seats);
        return reservation;
    }

    private void updateLockStatusToConverted(Showtime showtime, List<Seat> seats, User user, String sessionId) {
        for (Seat seat : seats) {
            int updatedRows;
            updatedRows = seatLockRepository.markLockAsConverted(showtime.getId(), seat.getId(), user != null ? user.getId() : null, sessionId);
            if (updatedRows == 0) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " is no longer available for conversion");
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

    // Helper function
    private void validateLockIdentity(User user, String guestEmail) {
        boolean hasUser = user != null;
        boolean hasGuestEmail = guestEmail != null && !guestEmail.isBlank();

        if (hasUser == hasGuestEmail) {
            throw new IllegalArgumentException(
                    "Exactly one of user or guestEmail must be provided"
            );
        }
    }

    private void validateLockIdentity(User user, String guestEmail, String sessionId) {
        boolean hasUser = user != null;
        boolean hasGuestEmail = guestEmail != null && !guestEmail.isBlank();
        boolean hasSessionId = sessionId != null && !sessionId.isBlank();

        if (hasUser == hasGuestEmail || (hasGuestEmail && !hasSessionId) || (hasUser && hasSessionId)) {
            throw new IllegalArgumentException(
                    "Exactly one of user or guestEmail must be provided, and sessionId must be provided for guest users and not provided for registered users"
            );
        }
    }
}
