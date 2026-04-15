package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.Exception.SeatUnavailableException;
import com.example.moviereservation.dto.CheckoutSessionCreateRequest;
import com.example.moviereservation.dto.CheckoutSessionCreateResponse;
import com.example.moviereservation.dto.CheckoutSessionStatusResponse;
import com.example.moviereservation.dto.StripeCheckoutCompletedEvent;
import com.example.moviereservation.dto.StripeCheckoutExpiredEvent;
import com.example.moviereservation.entity.CheckoutItemSnapshot;
import com.example.moviereservation.entity.CheckoutSession;
import com.example.moviereservation.entity.CheckoutSessionStatus;
import com.example.moviereservation.entity.CurrencyCode;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.SeatLock;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.ShowtimeStatus;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.CheckoutSessionRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.security.CustomUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import com.example.moviereservation.dto.StripeCheckoutSessionResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutSessionService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final SeatLockRepository seatLockRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final ObjectMapper objectMapper;
    private final StripeCheckoutService stripeCheckoutService;
    private final ReservationService reservationService;

    public CheckoutSessionService(
            ShowtimeRepository showtimeRepository,
            SeatRepository seatRepository,
            UserRepository userRepository,
            SeatLockRepository seatLockRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            ObjectMapper objectMapper,
            StripeCheckoutService stripeCheckoutService,
            ReservationService reservationService
    ) {
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.seatLockRepository = seatLockRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.objectMapper = objectMapper;
        this.stripeCheckoutService = stripeCheckoutService;
        this.reservationService = reservationService;
    }

    @Transactional
    // Canonical real-payment flow: create Stripe checkout for existing locks.
    // Reservation creation happens only after Stripe webhook confirmation.
    public CheckoutSessionCreateResponse createCheckoutSession(
            CheckoutSessionCreateRequest request,
            CustomUserPrincipal principal
    ) {
        Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());

        validateCheckoutSessionCreateRequest(request, effectiveUserId);

        CheckoutSessionContext context = prepareCheckoutSessionContext(
                request.getShowtimeId(),
                request.getSeatIds(),
                effectiveUserId
        );

        List<SeatLock> activeLocks = loadActiveLocks(
                context.getShowtime().getId(),
                request.getSeatIds(),
                context.getUser(),
                request.getSessionId(),
                request.getGuestEmail()
        );

        validateAllSeatsLocked(request.getSeatIds(), activeLocks);

        CheckoutSession checkoutSession = createPendingCheckoutSession(
                request,
                context,
                activeLocks
        );

        return buildCreateResponse(checkoutSession);
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

    private void validateCheckoutSessionCreateRequest(CheckoutSessionCreateRequest request, Long effectiveUserId) {
        validateCommonSeatRequest(request.getShowtimeId(), request.getSeatIds());
        validateSessionIdentity(effectiveUserId, request.getGuestEmail(), request.getSessionId());
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

    private void validateSessionIdentity(Long userId, String guestEmail, String sessionId) {
        boolean hasUserId = userId != null;
        boolean hasGuestEmail = guestEmail != null && !guestEmail.isBlank();
        boolean hasSessionId = sessionId != null && !sessionId.isBlank();

        if (hasUserId) {
            if (hasGuestEmail || hasSessionId) {
                throw new IllegalArgumentException("Guest identity must not be provided for authenticated users");
            }
            return;
        }

        if (!hasGuestEmail) {
            throw new IllegalArgumentException("Guest email is required for guest checkout");
        }

        if (!hasSessionId) {
            throw new IllegalArgumentException("Session ID is required for guest checkout");
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!guestEmail.matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private CheckoutSessionContext prepareCheckoutSessionContext(Long showtimeId, List<Long> seatIds, Long userId) {
        Showtime showtime = loadShowtime(showtimeId);
        List<Seat> seats = loadSeats(seatIds);
        User user = loadUser(userId);

        validateCommonCheckoutPreconditions(showtime, seats);

        return new CheckoutSessionContext(showtime, seats, user);
    }

    private Showtime loadShowtime(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));
    }

    private List<Seat> loadSeats(List<Long> seatIds) {
        return seatIds.stream()
                .sorted()
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

    private void validateCommonCheckoutPreconditions(Showtime showtime, List<Seat> seats) {
        validateShowtimeBookable(showtime);
        validateSeatsWithinLimit(seats);
        validateSeatsBelongToShowtime(showtime, seats);
    }

    private void validateShowtimeBookable(Showtime showtime) {
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new SeatUnavailableException("Cannot create checkout session for a showtime that has already started");
        }

        if (showtime.getStatus() != ShowtimeStatus.UPCOMING) {
            throw new SeatUnavailableException("Cannot create checkout session for a showtime that is not upcoming");
        }
    }

    private void validateSeatsWithinLimit(List<Seat> seats) {
        int seatLimit = 10;
        if (seats.size() > seatLimit) {
            throw new IllegalArgumentException("Cannot create checkout session for more than " + seatLimit + " seats");
        }
    }

    private void validateSeatsBelongToShowtime(Showtime showtime, List<Seat> seats) {
        for (Seat seat : seats) {
            if (!seat.getScreen().getId().equals(showtime.getScreen().getId())) {
                throw new IllegalArgumentException("Seat " + seat.getId() + " does not belong to the specified showtime");
            }
        }
    }

    private List<SeatLock> loadActiveLocks(
            Long showtimeId,
            List<Long> seatIds,
            User user,
            String sessionId,
            String guestEmail
    ) {
        if (user != null) {
            return seatLockRepository.findActiveLocksForUser(showtimeId, seatIds, user.getId());
        }

        return seatLockRepository.findActiveLocksForGuest(showtimeId, seatIds, sessionId, guestEmail);
    }

    private void validateAllSeatsLocked(List<Long> requestedSeatIds, List<SeatLock> activeLocks) {
        List<Long> lockedSeatIds = activeLocks.stream()
                .map(lock -> lock.getSeat().getId())
                .distinct()
                .sorted()
                .toList();

        List<Long> expectedSeatIds = requestedSeatIds.stream()
                .distinct()
                .sorted()
                .toList();

        if (!lockedSeatIds.equals(expectedSeatIds)) {
            throw new SeatUnavailableException("No valid active lock found for this checkout session request");
        }
    }

    private CheckoutSession createPendingCheckoutSession(
            CheckoutSessionCreateRequest request,
            CheckoutSessionContext context,
            List<SeatLock> activeLocks
    ) {
        CheckoutSession checkoutSession = new CheckoutSession();
        checkoutSession.setCheckoutReference(generateCheckoutReference());
        checkoutSession.setShowtime(context.getShowtime());
        checkoutSession.setUser(context.getUser());

        if (context.getUser() == null) {
            checkoutSession.setGuestEmail(request.getGuestEmail());
            checkoutSession.setGuestSessionId(request.getSessionId());
        }

        checkoutSession.setItemsSnapshotJson(buildItemsSnapshotJson(context.getSeats()));
        checkoutSession.setTotalAmount(calculateTotalAmount(context.getSeats()));
        checkoutSession.setCurrency(CurrencyCode.GBP);
        checkoutSession.setStatus(CheckoutSessionStatus.PENDING_PAYMENT);
        checkoutSession.setExpiresAt(resolveCheckoutExpiry(activeLocks));


        checkoutSession.setStripeCustomerEmail(resolveCustomerEmail(context.getUser(), request.getGuestEmail()));

        checkoutSession = checkoutSessionRepository.save(checkoutSession);

        StripeCheckoutSessionResult stripeResult =
                stripeCheckoutService.createHostedCheckoutSession(checkoutSession);

        checkoutSession.setStripeCheckoutSessionId(stripeResult.getCheckoutSessionId());
        checkoutSession.setStripePaymentIntentId(stripeResult.getPaymentIntentId());
        checkoutSession.setCheckoutUrl(stripeResult.getCheckoutUrl());

        return checkoutSessionRepository.save(checkoutSession);
    }

    private String buildItemsSnapshotJson(List<Seat> seats) {
        List<CheckoutItemSnapshot> items = seats.stream()
                .sorted(Comparator.comparing(Seat::getId))
                .map(seat -> new CheckoutItemSnapshot(
                        seat.getId(),
                        seat.getRowLabel(),
                        seat.getSeatNumber(),
                        seat.getSeatType().name(),
                        seat.getBasePrice()
                ))
                .toList();

        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize checkout item snapshot", e);
        }
    }

    private BigDecimal calculateTotalAmount(List<Seat> seats) {
        return seats.stream()
                .map(Seat::getBasePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime resolveCheckoutExpiry(List<SeatLock> activeLocks) {
        return activeLocks.stream()
                .map(SeatLock::getExpiresAt)
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new SeatUnavailableException("No valid active locks found"));
    }

    private String resolveCustomerEmail(User user, String guestEmail) {
        if (user != null) {
            return user.getEmail();
        }

        return guestEmail;
    }

    private CheckoutSessionCreateResponse buildCreateResponse(CheckoutSession checkoutSession) {
        return new CheckoutSessionCreateResponse(
                checkoutSession.getCheckoutReference(),
                checkoutSession.getStripeCheckoutSessionId(),
                checkoutSession.getCheckoutUrl(),
                checkoutSession.getStatus(),
                checkoutSession.getExpiresAt(),
                "Checkout session created successfully"
        );
    }

    private String generateCheckoutReference() {
        return "chk_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static class CheckoutSessionContext {
        private final Showtime showtime;
        private final List<Seat> seats;
        private final User user;

        private CheckoutSessionContext(Showtime showtime, List<Seat> seats, User user) {
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

    @Transactional(readOnly = true)
    public CheckoutSessionStatusResponse getCheckoutSessionStatus(
            String checkoutReference,
            CustomUserPrincipal principal
    ) {
        CheckoutSession checkoutSession = checkoutSessionRepository.findByCheckoutReference(checkoutReference)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found with reference: " + checkoutReference));

        validateCheckoutSessionStatusAccess(checkoutSession, principal);

        CheckoutSessionStatus effectiveStatus = resolveEffectiveStatus(checkoutSession);

        Long reservationId = checkoutSession.getReservation() != null
                ? checkoutSession.getReservation().getId()
                : null;

        String bookingReference = checkoutSession.getReservation() != null
                ? checkoutSession.getReservation().getBookingReference()
                : null;

        return new CheckoutSessionStatusResponse(
                checkoutSession.getCheckoutReference(),
                effectiveStatus,
                reservationId,
                bookingReference,
                buildStatusMessage(effectiveStatus)
        );
    }

    private void validateCheckoutSessionStatusAccess(CheckoutSession checkoutSession, CustomUserPrincipal principal) {
        if (checkoutSession.getUser() != null) {
            if (principal == null || !checkoutSession.getUser().getId().equals(principal.getUserId())) {
                throw new SeatUnavailableException("Checkout session does not belong to this user");
            }
            return;
        }

        // Guest status lookup currently relies on an unguessable checkoutReference.
        // If this becomes public-facing, add guestEmail/sessionId verification to the request contract.
    }

    private CheckoutSessionStatus resolveEffectiveStatus(CheckoutSession checkoutSession) {
        if (checkoutSession.getStatus() == CheckoutSessionStatus.PENDING_PAYMENT
                && LocalDateTime.now().isAfter(checkoutSession.getExpiresAt())) {
            return CheckoutSessionStatus.EXPIRED;
        }

        return checkoutSession.getStatus();
    }

    private String buildStatusMessage(CheckoutSessionStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "Checkout session is awaiting payment";
            case PAID -> "Payment completed and reservation finalization is pending";
            case FINALIZED -> "Checkout session finalized successfully";
            case FAILED -> "Checkout session payment failed";
            case CANCELLED -> "Checkout session was cancelled";
            case EXPIRED -> "Checkout session expired";
        };
    }


    // public method for handling stripe webhooks to update checkout session status based on payment events
    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        StripeCheckoutCompletedEvent completedEvent =
            stripeCheckoutService.parseCheckoutCompletedEvent(payload, signatureHeader);

        if (completedEvent != null) {
            finalizePaidCheckoutSession(completedEvent);
            return;
        }

        StripeCheckoutExpiredEvent expiredEvent =
                stripeCheckoutService.parseCheckoutExpiredEvent(payload, signatureHeader);

        if (expiredEvent != null) {
            expireCheckoutSession(expiredEvent);
        }
    }

    // private void markCheckoutSessionPaid(StripeCheckoutCompletedEvent completedEvent) {
    //     CheckoutSession checkoutSession = checkoutSessionRepository
    //             .findByStripeCheckoutSessionId(completedEvent.getStripeCheckoutSessionId())
    //             .orElse(null);

    //     if (checkoutSession == null) {
    //         return;
    //     }

    //     if (checkoutSession.getStatus() == CheckoutSessionStatus.PAID
    //             || checkoutSession.getStatus() == CheckoutSessionStatus.FINALIZED) {
    //         return;
    //     }

    //     checkoutSession.setStatus(CheckoutSessionStatus.PAID);
    //     checkoutSession.setStripePaymentIntentId(completedEvent.getStripePaymentIntentId());
    //     checkoutSession.setCompletedAt(LocalDateTime.now());

    //     checkoutSessionRepository.save(checkoutSession);
    // }

    private void finalizePaidCheckoutSession(StripeCheckoutCompletedEvent completedEvent) {
        CheckoutSession checkoutSession = checkoutSessionRepository
                .findByStripeCheckoutSessionId(completedEvent.getStripeCheckoutSessionId())
                .orElse(null);

        if (checkoutSession == null) {
            return;
        }

        if (checkoutSession.getStatus() == CheckoutSessionStatus.FINALIZED) {
            return;
        }

        if (checkoutSession.getReservation() != null) {
            checkoutSession.setStripePaymentIntentId(completedEvent.getStripePaymentIntentId());
            checkoutSession.setStatus(CheckoutSessionStatus.FINALIZED);
            checkoutSessionRepository.save(checkoutSession);
            return;
        }

        if (checkoutSession.getStatus() == CheckoutSessionStatus.CANCELLED
                || checkoutSession.getStatus() == CheckoutSessionStatus.EXPIRED
                || checkoutSession.getStatus() == CheckoutSessionStatus.FAILED) {
            markCheckoutFinalizationFailed(checkoutSession, completedEvent);
            return;
        }

        try {
            List<Seat> seats = loadSeatsFromCheckoutSnapshot(checkoutSession);

            validateLocksStillActiveForFinalization(checkoutSession, seats);

            Reservation reservation = reservationService.createPaidReservation(
                    checkoutSession.getUser(),
                    checkoutSession.getGuestEmail(),
                    checkoutSession.getShowtime(),
                    seats
            );

            convertLocksToReservation(checkoutSession, seats);

            checkoutSession.setReservation(reservation);
            checkoutSession.setStripePaymentIntentId(completedEvent.getStripePaymentIntentId());
            checkoutSession.setCompletedAt(LocalDateTime.now());
            checkoutSession.setStatus(CheckoutSessionStatus.FINALIZED);

            checkoutSessionRepository.save(checkoutSession);
        } catch (SeatUnavailableException | IllegalStateException e) {
            markCheckoutFinalizationFailed(checkoutSession, completedEvent);
        }
    }

    private void markCheckoutFinalizationFailed(
            CheckoutSession checkoutSession,
            StripeCheckoutCompletedEvent completedEvent
    ) {
        checkoutSession.setStripePaymentIntentId(completedEvent.getStripePaymentIntentId());
        checkoutSession.setFailedAt(LocalDateTime.now());
        checkoutSession.setStatus(CheckoutSessionStatus.FAILED);

        checkoutSessionRepository.save(checkoutSession);
    }


    private List<Seat> loadSeatsFromCheckoutSnapshot(CheckoutSession checkoutSession) {
        List<Long> seatIds = parseSeatIdsFromItemsSnapshot(checkoutSession.getItemsSnapshotJson());
        return loadSeats(seatIds);
    }

    private List<Long> parseSeatIdsFromItemsSnapshot(String itemsSnapshotJson) {
        try {
            CheckoutItemSnapshot[] items = objectMapper.readValue(itemsSnapshotJson, CheckoutItemSnapshot[].class);
            return java.util.Arrays.stream(items)
                    .map(CheckoutItemSnapshot::getSeatId)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialize checkout item snapshot", e);
        }
    }

    private void validateLocksStillActiveForFinalization(CheckoutSession checkoutSession, List<Seat> seats) {
        List<Long> seatIds = seats.stream()
                .map(Seat::getId)
                .sorted()
                .toList();

        List<SeatLock> activeLocks = loadActiveLocks(
                checkoutSession.getShowtime().getId(),
                seatIds,
                checkoutSession.getUser(),
                checkoutSession.getGuestSessionId(),
                checkoutSession.getGuestEmail()
        );

        validateAllSeatsLocked(seatIds, activeLocks);
    }

    private void convertLocksToReservation(CheckoutSession checkoutSession, List<Seat> seats) {
        for (Seat seat : seats) {
            int updatedRows = seatLockRepository.markActiveLockAsConverted(
                    checkoutSession.getShowtime().getId(),
                    seat.getId(),
                    checkoutSession.getUser() != null ? checkoutSession.getUser().getId() : null,
                    checkoutSession.getGuestSessionId(),
                    checkoutSession.getGuestEmail()
            );

            if (updatedRows == 0) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " could not be finalized for reservation");
            }
        }
    }

    private void expireCheckoutSession(StripeCheckoutExpiredEvent expiredEvent) {
        CheckoutSession checkoutSession = checkoutSessionRepository
                .findByStripeCheckoutSessionId(expiredEvent.getStripeCheckoutSessionId())
                .orElse(null);

        if (checkoutSession == null) {
            return;
        }

        if (checkoutSession.getStatus() == CheckoutSessionStatus.FINALIZED) {
            return;
        }

        if (checkoutSession.getStatus() == CheckoutSessionStatus.CANCELLED
                || checkoutSession.getStatus() == CheckoutSessionStatus.EXPIRED) {
            return;
        }

        checkoutSession.setStatus(CheckoutSessionStatus.EXPIRED);

        checkoutSessionRepository.save(checkoutSession);
    }

}
