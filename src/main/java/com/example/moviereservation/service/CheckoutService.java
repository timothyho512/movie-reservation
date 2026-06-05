package com.example.moviereservation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.moviereservation.dto.CheckoutLockRequest;
import com.example.moviereservation.dto.CheckoutLockResponse;
import com.example.moviereservation.entity.CheckoutLockIdempotencyKey;
import com.example.moviereservation.entity.CheckoutLockIdempotencyStatus;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.SeatLock;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.ShowtimeStatus;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.service.RedisSeatLockService.RedisSeatLockBatch;
import com.example.moviereservation.service.RedisSeatLockService.RedisSeatLockOwner;
import com.example.moviereservation.service.RedisSeatLockService.RedisSeatLockValue;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.CheckoutSessionRepository;
import com.example.moviereservation.repository.CheckoutLockIdempotencyKeyRepository;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.Exception.CheckoutExpiredException;
import com.example.moviereservation.Exception.IdempotencyConflictException;
import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.Exception.SeatUnavailableException;
import com.example.moviereservation.dto.CheckoutConfirmRequest;
import com.example.moviereservation.dto.CheckoutConfirmResponse;
import com.example.moviereservation.observability.CheckoutMetrics;

import com.example.moviereservation.dto.CancelLockRequest;
import com.example.moviereservation.dto.CancelLockResponse;

import com.example.moviereservation.security.CustomUserPrincipal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;


@Service
public class CheckoutService {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    private final ShowtimeRepository showtimeRepository;

    private final SeatRepository seatRepository;

    private final ReservationRepository reservationRepository;

    private final SeatLockRepository seatLockRepository;

    private final UserRepository userRepository;

    private final ReservationService reservationService;

    private final CheckoutSessionRepository checkoutSessionRepository;

    private final CheckoutLockIdempotencyKeyRepository checkoutLockIdempotencyKeyRepository;

    private final RedisSeatLockService redisSeatLockService;

    private final RedisSeatMapCacheService redisSeatMapCacheService;

    private final CheckoutMetrics checkoutMetrics;

    public CheckoutService(ShowtimeRepository showtimeRepository, SeatRepository seatRepository, ReservationRepository reservationRepository, SeatLockRepository seatLockRepository, UserRepository userRepository, ReservationService reservationService, StripeCheckoutService stripeCheckoutService, CheckoutSessionRepository checkoutSessionRepository, CheckoutLockIdempotencyKeyRepository checkoutLockIdempotencyKeyRepository, RedisSeatLockService redisSeatLockService, RedisSeatMapCacheService redisSeatMapCacheService, CheckoutMetrics checkoutMetrics) {
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.seatLockRepository = seatLockRepository;
        this.userRepository = userRepository;
        this.reservationService = reservationService;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.checkoutLockIdempotencyKeyRepository = checkoutLockIdempotencyKeyRepository;
        this.redisSeatLockService = redisSeatLockService;
        this.redisSeatMapCacheService = redisSeatMapCacheService;
        this.checkoutMetrics = checkoutMetrics;
    }

    // the whole thing needs to be transactional
    // validation, loading showtime/seats/user, availability check, lock creation, reservation creation and save
    // all happen inside one service transaction
    @Transactional
    public CheckoutLockResponse lockSeats(CheckoutLockRequest request, CustomUserPrincipal principal) {
        return lockSeats(request, principal, null);
    }

    @Transactional
    public CheckoutLockResponse lockSeats(
            CheckoutLockRequest request,
            CustomUserPrincipal principal,
            String idempotencyKey
    ) {
        checkoutMetrics.recordCheckoutLockAttempt();
        logger.info(
                "event=checkout_lock_started showtimeId={} seatCount={} authenticated={}",
                request.getShowtimeId(),
                request.getSeatIds() == null ? 0 : request.getSeatIds().size(),
                principal != null
        );

        try {
            String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
            Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());
            // Validate request (showtimeId, seatIds, guest/auth identity)
            validateCheckoutLockRequest(request, effectiveUserId);

            CheckoutContext context = prepareCheckoutContext(request.getShowtimeId(), request.getSeatIds(), effectiveUserId);

            String guestEmail = principal != null ? null : request.getGuestEmail();

            String requestFingerprint = normalizedIdempotencyKey == null
                    ? null
                    : buildLockRequestFingerprint(request, effectiveUserId, guestEmail);

            CheckoutLockIdempotencyKey lockIdempotencyKey = null;

            if (normalizedIdempotencyKey != null) {
                lockIdempotencyKey = findExistingLockIdempotencyKey(
                        normalizedIdempotencyKey,
                        effectiveUserId,
                        guestEmail
                );

                if (lockIdempotencyKey != null) {
                    validateLockIdempotencyFingerprint(lockIdempotencyKey, requestFingerprint);
                    CheckoutLockResponse replayResponse = buildCompletedLockResponseFromIdempotencyKey(lockIdempotencyKey);
                    if (replayResponse != null) {
                        checkoutMetrics.recordCheckoutLockSuccess();
                        logger.info(
                                "event=checkout_lock_succeeded showtimeId={} lockedSeatCount={} replay=true",
                                request.getShowtimeId(),
                                replayResponse.getLockedSeatIds().size()
                        );
                        return replayResponse;
                    }
                } else {
                    lockIdempotencyKey = createStartedLockIdempotencyKey(
                            normalizedIdempotencyKey,
                            requestFingerprint,
                            context,
                            guestEmail
                    );
                }
            }

            // Redis createLocks is the active lock authority and can recover same-owner retries.
            validateSeatsNotReserved(context.getShowtime(), context.getSeats());

            RedisSeatLockOwner owner = buildRedisOwnerForLock(context.getUser(), guestEmail, lockIdempotencyKey);
            RedisSeatLockBatch lockBatch;
            try {
                lockBatch = createLock(context.getShowtime(), context.getSeats(), owner);
            } catch (RuntimeException e) {
                markLockIdempotencyFailed(lockIdempotencyKey, e);
                throw e;
            }

            createAuditLocks(context.getUser(), guestEmail, context.getShowtime(), context.getSeats(), lockBatch);
            CheckoutLockResponse response = buildLockResponse(lockBatch);
            completeLockIdempotencyKey(lockIdempotencyKey, response);

            redisSeatMapCacheService.evict(context.getShowtime().getId());

            checkoutMetrics.recordCheckoutLockSuccess();
            logger.info(
                    "event=checkout_lock_succeeded showtimeId={} lockedSeatCount={} replay=false",
                    context.getShowtime().getId(),
                    response.getLockedSeatIds().size()
            );

            // Return lock details (sessionId, expiresAt, lockedSeatIds) to the client
            return response;
        } catch (RuntimeException e) {
            checkoutMetrics.recordCheckoutLockFailure(e);
            logger.warn(
                    "event=checkout_lock_failed showtimeId={} seatCount={} exception={}",
                    request.getShowtimeId(),
                    request.getSeatIds() == null ? 0 : request.getSeatIds().size(),
                    e.getClass().getSimpleName()
            );
            throw e;
        }
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }

        String trimmed = idempotencyKey.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key must not be blank");
        }

        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 255 characters");
        }

        return trimmed;
    }

    @Transactional
    // Legacy fake-payment confirmation flow. Do not use this as the production payment path.
    // Stripe-backed checkout finalizes reservations from CheckoutSessionService webhook handling.
    public CheckoutConfirmResponse confirmCheckout(CheckoutConfirmRequest request, CustomUserPrincipal principal) {
        logger.info(
                "event=legacy_checkout_confirm_started showtimeId={} seatCount={} authenticated={}",
                request.getShowtimeId(),
                request.getSeatIds() == null ? 0 : request.getSeatIds().size(),
                principal != null
        );
        Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());
        // Validate request (showtimeId, seatIds, guest/auth identity, sessionId)
        validateCheckoutConfirmRequest(request, effectiveUserId);
  
        CheckoutContext context = prepareCheckoutContext(request.getShowtimeId(), request.getSeatIds(), effectiveUserId);

        String sessionId = principal != null ? null : request.getSessionId();
        String guestEmail = principal != null ? null : request.getGuestEmail();
        RedisSeatLockOwner owner = buildRedisOwnerForExistingLock(context.getUser(), sessionId, guestEmail);

        validateLocksOwned(context.getShowtime(), context.getSeats(), owner);

        // If valid, create a reservation in the database
        try {
            processPayment(request.getPaymentMethodToken());
            Reservation reservation = createReservation(context.getUser(), guestEmail, context.getShowtime(), context.getSeats());

            updateLockStatusToConverted(context.getShowtime(), context.getSeats(), context.getUser(), sessionId, guestEmail);
            releaseLocks(context.getShowtime(), context.getSeats(), owner);
            redisSeatMapCacheService.evict(context.getShowtime().getId());

            CheckoutConfirmResponse response = buildConfirmResponse(reservation);
            logger.info(
                    "event=legacy_checkout_confirm_succeeded showtimeId={} reservationId={}",
                    context.getShowtime().getId(),
                    reservation.getId()
            );
            return response;
        } catch (DataIntegrityViolationException e) {
            // If not available, return an error
            logger.warn(
                    "event=legacy_checkout_confirm_failed showtimeId={} exception={}",
                    request.getShowtimeId(),
                    e.getClass().getSimpleName()
            );
            throw new SeatUnavailableException("One or more seats are no longer available");
        }
    }

    @Transactional
    public CancelLockResponse cancelLock(CancelLockRequest request, CustomUserPrincipal principal) {
        logger.info(
                "event=checkout_lock_cancel_started showtimeId={} seatCount={} authenticated={}",
                request.getShowtimeId(),
                request.getSeatIds() == null ? 0 : request.getSeatIds().size(),
                principal != null
        );
        Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());
        // Validate request (showtimeId, seatIds, guest/auth identity, sessionId)
        validateCancelLockRequest(request, effectiveUserId);

        CheckoutContext context = prepareCheckoutContext(request.getShowtimeId(), request.getSeatIds(), effectiveUserId);

        String sessionId = principal != null ? null : request.getSessionId();
        String guestEmail = principal != null ? null : request.getGuestEmail();
        RedisSeatLockOwner owner = buildRedisOwnerForExistingLock(context.getUser(), sessionId, guestEmail);

        cancelLocks(context.getShowtime(), context.getSeats(), owner);
        expireAuditLocks(context.getShowtime(), context.getSeats(), context.getUser(), sessionId, guestEmail);
        cancelPendingCheckoutSessions(context.getShowtime(), context.getUser(), sessionId, guestEmail);
        redisSeatMapCacheService.evict(context.getShowtime().getId());

        logger.info(
                "event=checkout_lock_cancel_succeeded showtimeId={} seatCount={}",
                context.getShowtime().getId(),
                context.getSeats().size()
        );
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
        validatePaymentMethodToken(request.getPaymentMethodToken());
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
            boolean isLocked = redisSeatLockService.findLockedSeatIdsForShowtime(showtime.getId()).contains(seat.getId());
            if (isLocked) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " is currently locked by another user for this showtime");
            }
        }

        // If any seat is not available, throw an exception with details about which seats are unavailable
    }

    private void validateSeatsNotReserved(Showtime showtime, List<Seat> seats) {
        for (Seat seat : seats) {
            boolean isReserved = reservationRepository.existsReservedSeatForShowtime(showtime.getId(), seat.getId());
            if (isReserved) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " is already reserved for this showtime");
            }
        }
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

    private void validateLocksOwned(Showtime showtime, List<Seat> seats, RedisSeatLockOwner owner) {
        List<Long> requestedSeatIds = seats.stream().map(Seat::getId).sorted().toList();
        List<Long> ownedSeatIds = redisSeatLockService.findOwnedLocks(showtime.getId(), requestedSeatIds, owner).stream()
                .map(RedisSeatLockValue::seatId)
                .sorted()
                .toList();

        if (!ownedSeatIds.equals(requestedSeatIds)) {
            Long unavailableSeatId = requestedSeatIds.stream()
                    .filter(seatId -> !ownedSeatIds.contains(seatId))
                    .findFirst()
                    .orElse(requestedSeatIds.getFirst());
            throw new SeatUnavailableException("No valid active lock found for seat " + unavailableSeatId + " for this confirmation request");
        }
    }

    private void cancelLocks(Showtime showtime, List<Seat> seats, RedisSeatLockOwner owner) {
        int releasedLocks = releaseLocks(showtime, seats, owner);
        if (releasedLocks != seats.size()) {
            throw new SeatUnavailableException("No valid active lock found for this cancellation request");
        }
    }

    private RedisSeatLockBatch createLock(Showtime showtime, List<Seat> seats, RedisSeatLockOwner owner) {
        return redisSeatLockService.createLocks(showtime.getId(), seats.stream().map(Seat::getId).toList(), owner);
    }

    private void createAuditLocks(
            User user,
            String guestEmail,
            Showtime showtime,
            List<Seat> seats,
            RedisSeatLockBatch lockBatch
    ) {
        List<SeatLock> locks = seats.stream()
                .filter(seat -> !seatLockRepository.existsActiveLockForOwner(
                        showtime.getId(),
                        seat.getId(),
                        user != null ? user.getId() : null,
                        lockBatch.sessionId(),
                        guestEmail
                ))
                .map(seat -> {
                    SeatLock seatLock = new SeatLock();
                    seatLock.setShowtime(showtime);
                    seatLock.setSeat(seat);
                    seatLock.setExpiresAt(lockBatch.expiresAt());

                    if (user != null) {
                        seatLock.setUser(user);
                    } else {
                        seatLock.setSessionId(lockBatch.sessionId());
                        seatLock.setGuestEmail(guestEmail);
                    }

                    return seatLock;
                })
                .toList();

        if (locks.isEmpty()) {
            return;
        }

        try {
            seatLockRepository.saveAll(locks);
        } catch (DataIntegrityViolationException ignored) {
            // Redis is the active lock authority. Postgres rows are kept as legacy audit only.
        }
    }

    private Reservation createReservation(User user, String guestEmail, Showtime showtime, List<Seat> seats) {
        Reservation reservation = reservationService.createReservation(user, guestEmail, showtime, seats);
        return reservation;
    }

    private int releaseLocks(Showtime showtime, List<Seat> seats, RedisSeatLockOwner owner) {
        return redisSeatLockService.releaseLocks(showtime.getId(), seats.stream().map(Seat::getId).toList(), owner);
    }

    private void updateLockStatusToConverted(Showtime showtime, List<Seat> seats, User user, String sessionId, String guestEmail) {
        for (Seat seat : seats) {
            seatLockRepository.markActiveLockAsConverted(
                    showtime.getId(),
                    seat.getId(),
                    user != null ? user.getId() : null,
                    sessionId,
                    guestEmail
            );
        }
    }

    private void expireAuditLocks(Showtime showtime, List<Seat> seats, User user, String sessionId, String guestEmail) {
        for (Seat seat : seats) {
            seatLockRepository.cancelActiveLock(
                    showtime.getId(),
                    seat.getId(),
                    user != null ? user.getId() : null,
                    sessionId,
                    guestEmail
            );
        }
    }

    // helper function
    private CheckoutLockResponse buildLockResponse(RedisSeatLockBatch lockBatch) {
        return new CheckoutLockResponse(lockBatch.sessionId(), lockBatch.expiresAt(), lockBatch.lockedSeatIds(), "Seats locked successfully");
    }

    private RedisSeatLockOwner buildRedisOwnerForLock(
            User user,
            String guestEmail,
            CheckoutLockIdempotencyKey lockIdempotencyKey
    ) {
        if (user != null) {
            return redisSeatLockService.authenticatedOwner(user.getId());
        }

        if (lockIdempotencyKey != null) {
            return redisSeatLockService.guestOwner(lockIdempotencyKey.getSessionId(), guestEmail);
        }

        return redisSeatLockService.guestOwner(guestEmail);
    }

    private RedisSeatLockOwner buildRedisOwnerForExistingLock(User user, String sessionId, String guestEmail) {
        if (user != null) {
            return redisSeatLockService.authenticatedOwner(user.getId());
        }

        return redisSeatLockService.guestOwner(sessionId, guestEmail);
    }

    private CheckoutConfirmResponse buildConfirmResponse(Reservation reservation) {
        return new CheckoutConfirmResponse(reservation.getId(),
                                            reservation.getBookingReference(),
                                            reservation.getStatus(),
                                            reservation.getPaymentStatus(),
                                            reservation.getTotalPrice(),
                                            reservation.getSeats().stream().map(Seat::getId).toList(),
                                            "Reservation confirmed successfully");
    }

    private CancelLockResponse buildCancelResponse(String message) {
        return new CancelLockResponse(message);
    }

    private void cancelPendingCheckoutSessions(Showtime showtime, User user, String sessionId, String guestEmail) {
        if (user != null) {
            checkoutSessionRepository.cancelPendingSessionsForUser(showtime.getId(), user.getId());
            return;
        }

        checkoutSessionRepository.cancelPendingSessionsForGuest(showtime.getId(), sessionId, guestEmail);
    }

    private CheckoutLockIdempotencyKey findExistingLockIdempotencyKey(
            String idempotencyKey,
            Long effectiveUserId,
            String guestEmail
    ) {
        if (effectiveUserId != null) {
            return checkoutLockIdempotencyKeyRepository
                    .findFirstByUserIdAndIdempotencyKey(effectiveUserId, idempotencyKey)
                    .orElse(null);
        }

        return checkoutLockIdempotencyKeyRepository
                .findFirstByGuestEmailAndIdempotencyKey(guestEmail.trim().toLowerCase(), idempotencyKey)
                .orElse(null);
    }

    private void validateLockIdempotencyFingerprint(
            CheckoutLockIdempotencyKey existingKey,
            String requestFingerprint
    ) {
        if (!requestFingerprint.equals(existingKey.getRequestFingerprint())) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key was already used for a different checkout lock request"
            );
        }
    }

    private CheckoutLockResponse buildCompletedLockResponseFromIdempotencyKey(
            CheckoutLockIdempotencyKey existingKey
    ) {
        if (existingKey.getStatus() != CheckoutLockIdempotencyStatus.COMPLETED) {
            return null;
        }

        if (LocalDateTime.now().isAfter(existingKey.getExpiresAt())) {
            throw new CheckoutExpiredException("Checkout lock idempotency key has expired");
        }

        return new CheckoutLockResponse(
                existingKey.getSessionId(),
                existingKey.getExpiresAt(),
                parseLockedSeatIds(existingKey.getLockedSeatIds()),
                "Seats locked successfully"
        );
    }

    private CheckoutLockIdempotencyKey createStartedLockIdempotencyKey(
            String idempotencyKey,
            String requestFingerprint,
            CheckoutContext context,
            String guestEmail
    ) {
        CheckoutLockIdempotencyKey lockIdempotencyKey = new CheckoutLockIdempotencyKey();
        lockIdempotencyKey.setIdempotencyKey(idempotencyKey);
        lockIdempotencyKey.setOwnerType(context.getUser() != null ? "USER" : "GUEST");
        lockIdempotencyKey.setUser(context.getUser());
        lockIdempotencyKey.setGuestEmail(guestEmail != null ? guestEmail.trim().toLowerCase() : null);
        lockIdempotencyKey.setRequestFingerprint(requestFingerprint);
        lockIdempotencyKey.setStatus(CheckoutLockIdempotencyStatus.STARTED);
        lockIdempotencyKey.setShowtime(context.getShowtime());
        if (context.getUser() == null) {
            lockIdempotencyKey.setSessionId(UUID.randomUUID().toString());
        }

        return checkoutLockIdempotencyKeyRepository.save(lockIdempotencyKey);
    }

    private void completeLockIdempotencyKey(
            CheckoutLockIdempotencyKey lockIdempotencyKey,
            CheckoutLockResponse response
    ) {
        if (lockIdempotencyKey == null) {
            return;
        }

        lockIdempotencyKey.setStatus(CheckoutLockIdempotencyStatus.COMPLETED);
        lockIdempotencyKey.setSessionId(response.getSessionId());
        lockIdempotencyKey.setExpiresAt(response.getExpiresAt());
        lockIdempotencyKey.setLockedSeatIds(writeLockedSeatIds(response.getLockedSeatIds()));
        lockIdempotencyKey.setLastError(null);

        checkoutLockIdempotencyKeyRepository.save(lockIdempotencyKey);
    }

    private void markLockIdempotencyFailed(
            CheckoutLockIdempotencyKey lockIdempotencyKey,
            RuntimeException exception
    ) {
        if (lockIdempotencyKey == null) {
            return;
        }

        lockIdempotencyKey.setStatus(CheckoutLockIdempotencyStatus.FAILED);
        lockIdempotencyKey.setLastError(truncateError(exception));
        checkoutLockIdempotencyKeyRepository.save(lockIdempotencyKey);
    }

    private String truncateError(RuntimeException exception) {
        String message = exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getName();
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    private String buildLockRequestFingerprint(
            CheckoutLockRequest request,
            Long effectiveUserId,
            String guestEmail
    ) {
        String owner = effectiveUserId != null
                ? "user:" + effectiveUserId
                : "guest:" + guestEmail.trim().toLowerCase();

        String sortedSeatIds = request.getSeatIds().stream()
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        return sha256Hex(owner
                + "|showtime:" + request.getShowtimeId()
                + "|seats:" + sortedSeatIds);
    }

    private String writeLockedSeatIds(List<Long> lockedSeatIds) {
        return lockedSeatIds.stream()
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private List<Long> parseLockedSeatIds(String lockedSeatIdsJson) {
        if (lockedSeatIdsJson == null || lockedSeatIdsJson.isBlank()) {
            return List.of();
        }

        return List.of(lockedSeatIdsJson.split(",")).stream()
                .map(Long::parseLong)
                .sorted()
                .toList();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not build idempotency fingerprint", e);
        }
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

    // ======= helper function for payment =======
    private void validatePaymentMethodToken(String paymentMethodToken) {
        if (paymentMethodToken == null || paymentMethodToken.isBlank()) {
            throw new IllegalArgumentException("Payment method token is required");
        }

        if (!paymentMethodToken.equals("pm_success") && !paymentMethodToken.equals("pm_fail")) {
            throw new IllegalArgumentException("Unsupported payment method token");
        }
    }

    private boolean isPaymentSuccessful(String paymentMethodToken) {
        return "pm_success".equals(paymentMethodToken);
    }

    private void processPayment(String paymentMethodToken) {
        if (!isPaymentSuccessful(paymentMethodToken)) {
            throw new SeatUnavailableException("Payment failed");
        }
    }

}
