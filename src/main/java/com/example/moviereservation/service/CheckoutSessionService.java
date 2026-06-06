package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.Exception.SeatUnavailableException;
import com.example.moviereservation.Exception.IdempotencyConflictException;
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
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.ShowtimeStatus;
import com.example.moviereservation.entity.User;
import com.example.moviereservation.entity.Reservation;
import com.example.moviereservation.repository.CheckoutSessionRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.repository.SeatRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import com.example.moviereservation.repository.UserRepository;
import com.example.moviereservation.service.OutboxEventService;
import com.example.moviereservation.security.CustomUserPrincipal;
import com.example.moviereservation.observability.CheckoutMetrics;
import com.example.moviereservation.service.RedisSeatLockService.RedisSeatLockOwner;
import com.example.moviereservation.service.RedisSeatLockService.RedisSeatLockValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import com.example.moviereservation.dto.StripeCheckoutSessionResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.HexFormat;

@Service
public class CheckoutSessionService {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutSessionService.class);

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final SeatLockRepository seatLockRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final ObjectMapper objectMapper;
    private final StripeCheckoutService stripeCheckoutService;
    private final ReservationService reservationService;
    private final RedisSeatLockService redisSeatLockService;
    private final RedisSeatMapCacheService redisSeatMapCacheService;
    private final OutboxEventService outboxEventService;
    private final CheckoutMetrics checkoutMetrics;
    private final BookingWindowService bookingWindowService;

    public CheckoutSessionService(
            ShowtimeRepository showtimeRepository,
            SeatRepository seatRepository,
            UserRepository userRepository,
            SeatLockRepository seatLockRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            ObjectMapper objectMapper,
            StripeCheckoutService stripeCheckoutService,
            ReservationService reservationService,
            RedisSeatLockService redisSeatLockService,
            RedisSeatMapCacheService redisSeatMapCacheService,
            OutboxEventService outboxEventService,
            CheckoutMetrics checkoutMetrics,
            BookingWindowService bookingWindowService
    ) {
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.seatLockRepository = seatLockRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.objectMapper = objectMapper;
        this.stripeCheckoutService = stripeCheckoutService;
        this.reservationService = reservationService;
        this.redisSeatLockService = redisSeatLockService;
        this.redisSeatMapCacheService = redisSeatMapCacheService;
        this.outboxEventService = outboxEventService;
        this.checkoutMetrics = checkoutMetrics;
        this.bookingWindowService = bookingWindowService;
    }

    @Transactional
    // Canonical real-payment flow: create Stripe checkout for existing locks.
    // Reservation creation happens only after Stripe webhook confirmation.
    public CheckoutSessionCreateResponse createCheckoutSession(
            CheckoutSessionCreateRequest request,
            CustomUserPrincipal principal
    ) {
        return createCheckoutSession(request, principal, null);
    }

    @Transactional
    public CheckoutSessionCreateResponse createCheckoutSession(
            CheckoutSessionCreateRequest request,
            CustomUserPrincipal principal,
            String idempotencyKey
    ) {
        logger.info(
                "event=checkout_session_creation_started showtimeId={} seatCount={} authenticated={}",
                request.getShowtimeId(),
                request.getSeatIds() == null ? 0 : request.getSeatIds().size(),
                principal != null
        );

        try {
            return checkoutMetrics.recordCheckoutSessionCreation(() -> {
                String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
                Long effectiveUserId = resolveAuthenticatedUserId(principal, request.getGuestEmail());

                validateCheckoutSessionCreateRequest(request, effectiveUserId);

                CheckoutSessionContext context = prepareCheckoutSessionContext(
                        request.getShowtimeId(),
                        request.getSeatIds(),
                        effectiveUserId
                );

                RedisSeatLockOwner owner = buildRedisOwner(context.getUser(), request.getSessionId(), request.getGuestEmail());
                List<RedisSeatLockValue> activeLocks = loadActiveLocks(
                        context.getShowtime().getId(),
                        request.getSeatIds(),
                        owner
                );

                validateAllSeatsLocked(request.getSeatIds(), activeLocks);

                String requestFingerprint = normalizedIdempotencyKey == null
                        ? null
                        : buildIdempotencyRequestFingerprint(request, effectiveUserId);

                if (normalizedIdempotencyKey != null) {
                    CheckoutSession existingSession = findExistingIdempotentCheckoutSession(
                            normalizedIdempotencyKey,
                            effectiveUserId,
                            request
                    );

                    if (existingSession != null) {
                        validateIdempotencyFingerprint(existingSession, requestFingerprint);
                        CheckoutSessionCreateResponse response = buildCreateResponse(ensureStripeCheckoutSession(existingSession));
                        logger.info(
                                "event=checkout_session_creation_succeeded showtimeId={} replay=true",
                                request.getShowtimeId()
                        );
                        return response;
                    }
                }

                CheckoutSession checkoutSession = createPendingCheckoutSession(
                        request,
                        context,
                        activeLocks,
                        normalizedIdempotencyKey,
                        requestFingerprint
                );

                CheckoutSessionCreateResponse response = buildCreateResponse(checkoutSession);
                logger.info(
                        "event=checkout_session_creation_succeeded showtimeId={} replay=false",
                        context.getShowtime().getId()
                );
                return response;
            });
        } catch (RuntimeException e) {
            checkoutMetrics.recordCheckoutSessionCreationFailure();
            logger.warn(
                    "event=checkout_session_creation_failed showtimeId={} seatCount={} exception={}",
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
        if (!bookingWindowService.isBookable(showtime, LocalDateTime.now())) {
            throw new SeatUnavailableException(
                    "Booking closes " + bookingWindowService.cutoffMinutes() + " minutes before showtime"
            );
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

    private List<RedisSeatLockValue> loadActiveLocks(
            Long showtimeId,
            List<Long> seatIds,
            RedisSeatLockOwner owner
    ) {
        return redisSeatLockService.findOwnedLocks(showtimeId, seatIds, owner);
    }

    private void validateAllSeatsLocked(List<Long> requestedSeatIds, List<RedisSeatLockValue> activeLocks) {
        List<Long> lockedSeatIds = activeLocks.stream()
                .map(RedisSeatLockValue::seatId)
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
            List<RedisSeatLockValue> activeLocks,
            String idempotencyKey,
            String requestFingerprint
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
        checkoutSession.setIdempotencyKey(idempotencyKey);
        checkoutSession.setIdempotencyRequestFingerprint(requestFingerprint);


        checkoutSession.setStripeCustomerEmail(resolveCustomerEmail(context.getUser(), request.getGuestEmail()));

        checkoutSession = checkoutSessionRepository.save(checkoutSession);

        return ensureStripeCheckoutSession(checkoutSession);
    }

    private CheckoutSession findExistingIdempotentCheckoutSession(
            String idempotencyKey,
            Long effectiveUserId,
            CheckoutSessionCreateRequest request
    ) {
        if (effectiveUserId != null) {
            return checkoutSessionRepository
                    .findFirstByUserIdAndIdempotencyKey(effectiveUserId, idempotencyKey)
                    .orElse(null);
        }

        return checkoutSessionRepository
                .findFirstByGuestEmailAndGuestSessionIdAndIdempotencyKey(
                        request.getGuestEmail(),
                        request.getSessionId(),
                        idempotencyKey
                )
                .orElse(null);
    }

    private void validateIdempotencyFingerprint(
            CheckoutSession checkoutSession,
            String requestFingerprint
    ) {
        if (!requestFingerprint.equals(checkoutSession.getIdempotencyRequestFingerprint())) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key was already used for a different checkout session request"
            );
        }
    }

    private CheckoutSession ensureStripeCheckoutSession(CheckoutSession checkoutSession) {
        if (checkoutSession.getStripeCheckoutSessionId() != null
                && !checkoutSession.getStripeCheckoutSessionId().isBlank()
                && checkoutSession.getCheckoutUrl() != null
                && !checkoutSession.getCheckoutUrl().isBlank()) {
            return checkoutSession;
        }

        StripeCheckoutSessionResult stripeResult =
                stripeCheckoutService.createHostedCheckoutSession(
                        checkoutSession,
                        buildStripeIdempotencyKey(checkoutSession)
                );

        checkoutSession.setStripeCheckoutSessionId(stripeResult.getCheckoutSessionId());
        checkoutSession.setStripePaymentIntentId(stripeResult.getPaymentIntentId());
        checkoutSession.setCheckoutUrl(stripeResult.getCheckoutUrl());

        return checkoutSessionRepository.save(checkoutSession);
    }

    private String buildIdempotencyRequestFingerprint(
            CheckoutSessionCreateRequest request,
            Long effectiveUserId
    ) {
        String owner = effectiveUserId != null
                ? "user:" + effectiveUserId
                : "guest:" + request.getGuestEmail().trim().toLowerCase() + ":" + request.getSessionId();

        String sortedSeatIds = request.getSeatIds().stream()
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        return sha256Hex(owner
                + "|showtime:" + request.getShowtimeId()
                + "|seats:" + sortedSeatIds);
    }

    private String buildStripeIdempotencyKey(CheckoutSession checkoutSession) {
        if (checkoutSession.getIdempotencyKey() == null || checkoutSession.getIdempotencyKey().isBlank()) {
            return null;
        }

        String owner = checkoutSession.getUser() != null
                ? "user:" + checkoutSession.getUser().getId()
                : "guest:" + checkoutSession.getGuestEmail().trim().toLowerCase()
                        + ":" + checkoutSession.getGuestSessionId();

        return "checkout-session-" + sha256Hex(owner + "|" + checkoutSession.getIdempotencyKey());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not build idempotency fingerprint", e);
        }
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

    private LocalDateTime resolveCheckoutExpiry(List<RedisSeatLockValue> activeLocks) {
        return activeLocks.stream()
                .map(RedisSeatLockValue::expiresAt)
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
            String guestEmail,
            String sessionId,
            CustomUserPrincipal principal
    ) {
        CheckoutSession checkoutSession = checkoutSessionRepository.findByCheckoutReference(checkoutReference)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found with reference: " + checkoutReference));

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

    private void validateCheckoutSessionStatusAccess(
            CheckoutSession checkoutSession,
            String guestEmail,
            String sessionId,
            CustomUserPrincipal principal
    ) {
        if (checkoutSession.getUser() != null) {
            if (guestEmail != null && !guestEmail.isBlank()
                    || sessionId != null && !sessionId.isBlank()) {
                throw new IllegalArgumentException("Guest identity must not be provided for authenticated checkout sessions");
            }

            if (principal == null || !checkoutSession.getUser().getId().equals(principal.getUserId())) {
                throw new SeatUnavailableException("Checkout session does not belong to this user");
            }
            return;
        }

        if (principal != null) {
            throw new SeatUnavailableException("Checkout session does not belong to this user");
        }

        if (guestEmail == null || guestEmail.isBlank()) {
            throw new IllegalArgumentException("Guest email is required for guest checkout status lookup");
        }

        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID is required for guest checkout status lookup");
        }

        if (checkoutSession.getGuestEmail() == null
                || checkoutSession.getGuestSessionId() == null
                || !checkoutSession.getGuestEmail().trim().equalsIgnoreCase(guestEmail.trim())
                || !checkoutSession.getGuestSessionId().equals(sessionId)) {
            throw new SeatUnavailableException("Checkout session does not belong to this guest");
        }
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
            case REFUND_PENDING -> "Payment received but reservation failed; refund is pending";
            case REFUNDED -> "Payment was refunded because the reservation could not be finalized";
        };
    }


    // public method for handling stripe webhooks to update checkout session status based on payment events
    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        checkoutMetrics.recordPaymentWebhookReceived();
        logger.info("event=payment_webhook_received provider=stripe");

        try {
            StripeCheckoutCompletedEvent completedEvent =
                stripeCheckoutService.parseCheckoutCompletedEvent(payload, signatureHeader);

            if (completedEvent != null) {
                finalizePaidCheckoutSession(completedEvent);
                checkoutMetrics.recordPaymentWebhookSuccess();
                logger.info(
                        "event=payment_webhook_processed provider=stripe type=checkout.session.completed"
                );
                return;
            }

            StripeCheckoutExpiredEvent expiredEvent =
                    stripeCheckoutService.parseCheckoutExpiredEvent(payload, signatureHeader);

            if (expiredEvent != null) {
                expireCheckoutSession(expiredEvent);
                checkoutMetrics.recordPaymentWebhookSuccess();
                logger.info(
                        "event=payment_webhook_processed provider=stripe type=checkout.session.expired"
                );
                return;
            }

            logger.info("event=payment_webhook_ignored provider=stripe reason=unsupported_event_type");
            checkoutMetrics.recordPaymentWebhookSuccess();
        } catch (RuntimeException e) {
            checkoutMetrics.recordPaymentWebhookFailure();
            logger.warn(
                    "event=payment_webhook_failed provider=stripe exception={}",
                    e.getClass().getSimpleName()
            );
            throw e;
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
                .findByStripeCheckoutSessionIdForUpdate(completedEvent.getStripeCheckoutSessionId())
                .orElse(null);

        if (checkoutSession == null) {
            logger.info(
                    "event=reservation_finalization_ignored reason=checkout_session_not_found provider=stripe"
            );
            return;
        }

        if (checkoutSession.getStatus() == CheckoutSessionStatus.FINALIZED) {
            logger.info(
                    "event=reservation_finalization_ignored checkoutSessionId={} reason=already_finalized",
                    checkoutSession.getId()
            );
            return;
        }

        if (checkoutSession.getStatus() == CheckoutSessionStatus.REFUNDED) {
            logger.info(
                    "event=reservation_finalization_ignored checkoutSessionId={} reason=already_refunded",
                    checkoutSession.getId()
            );
            return;
        }

        if (checkoutSession.getReservation() != null) {
            checkoutSession.setStripePaymentIntentId(completedEvent.getStripePaymentIntentId());
            checkoutSession.setStatus(CheckoutSessionStatus.FINALIZED);
            checkoutSessionRepository.save(checkoutSession);
            logger.info(
                    "event=reservation_finalization_ignored checkoutSessionId={} reason=reservation_already_exists",
                    checkoutSession.getId()
            );
            return;
        }

        if (checkoutSession.getStatus() == CheckoutSessionStatus.CANCELLED
                || checkoutSession.getStatus() == CheckoutSessionStatus.EXPIRED
                || checkoutSession.getStatus() == CheckoutSessionStatus.FAILED
                || checkoutSession.getStatus() == CheckoutSessionStatus.REFUND_PENDING) {
            scheduleRefund(checkoutSession, completedEvent);
            checkoutMetrics.recordReservationFinalizationFailure();
            logger.warn(
                    "event=reservation_finalization_refund_required checkoutSessionId={} status={} reason=terminal_checkout_status",
                    checkoutSession.getId(),
                    checkoutSession.getStatus()
            );
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

            updateAuditLocksToConverted(checkoutSession, seats);
            convertLocksToReservation(checkoutSession, seats);

            checkoutSession.setReservation(reservation);
            checkoutSession.setStripePaymentIntentId(completedEvent.getStripePaymentIntentId());
            checkoutSession.setCompletedAt(LocalDateTime.now());
            checkoutSession.setStatus(CheckoutSessionStatus.FINALIZED);

            checkoutSessionRepository.save(checkoutSession);
            outboxEventService.recordCheckoutPaymentFinalized(checkoutSession);
            redisSeatMapCacheService.evict(checkoutSession.getShowtime().getId());
            checkoutMetrics.recordReservationFinalizationSuccess();
            logger.info(
                    "event=reservation_finalized checkoutSessionId={} reservationId={} showtimeId={} seatCount={}",
                    checkoutSession.getId(),
                    reservation.getId(),
                    checkoutSession.getShowtime().getId(),
                    seats.size()
            );
        } catch (SeatUnavailableException | IllegalStateException e) {
            scheduleRefund(checkoutSession, completedEvent);
            checkoutMetrics.recordReservationFinalizationFailure();
            logger.warn(
                    "event=reservation_finalization_refund_required checkoutSessionId={} exception={}",
                    checkoutSession.getId(),
                    e.getClass().getSimpleName()
            );
        }
    }

    private void scheduleRefund(
            CheckoutSession checkoutSession,
            StripeCheckoutCompletedEvent completedEvent
    ) {
        checkoutSession.setStripePaymentIntentId(completedEvent.getStripePaymentIntentId());
        checkoutSession.setFailedAt(LocalDateTime.now());
        checkoutSession.setStatus(CheckoutSessionStatus.REFUND_PENDING);
        checkoutSessionRepository.save(checkoutSession);
        retryRefund(checkoutSession);
    }

    @Transactional
    public void retryRefund(CheckoutSession checkoutSession) {
        if (checkoutSession.getStatus() != CheckoutSessionStatus.REFUND_PENDING) {
            return;
        }

        try {
            String refundId = stripeCheckoutService.refundPaymentIntent(
                    checkoutSession.getStripePaymentIntentId(),
                    "checkout-refund-" + checkoutSession.getCheckoutReference()
            );
            checkoutSession.setStripeRefundId(refundId);
            checkoutSession.setRefundedAt(LocalDateTime.now());
            checkoutSession.setRefundError(null);
            checkoutSession.setStatus(CheckoutSessionStatus.REFUNDED);
        } catch (RuntimeException e) {
            checkoutSession.setRefundError(truncateError(e));
        }

        checkoutSessionRepository.save(checkoutSession);
    }

    private String truncateError(RuntimeException exception) {
        String message = exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getName();
        return message.length() <= 2000 ? message : message.substring(0, 2000);
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

        RedisSeatLockOwner owner = buildRedisOwner(
                checkoutSession.getUser(),
                checkoutSession.getGuestSessionId(),
                checkoutSession.getGuestEmail()
        );

        List<RedisSeatLockValue> activeLocks = loadActiveLocks(
                checkoutSession.getShowtime().getId(),
                seatIds,
                owner
        );

        validateAllSeatsLocked(seatIds, activeLocks);
    }

    private void convertLocksToReservation(CheckoutSession checkoutSession, List<Seat> seats) {
        RedisSeatLockOwner owner = buildRedisOwner(
                checkoutSession.getUser(),
                checkoutSession.getGuestSessionId(),
                checkoutSession.getGuestEmail()
        );
        List<Long> seatIds = seats.stream().map(Seat::getId).toList();
        int releasedLocks = redisSeatLockService.releaseLocks(checkoutSession.getShowtime().getId(), seatIds, owner);

        if (releasedLocks != seatIds.size()) {
            throw new SeatUnavailableException("One or more seats could not be finalized for reservation");
        }
    }

    private void expireCheckoutSession(StripeCheckoutExpiredEvent expiredEvent) {
        CheckoutSession checkoutSession = checkoutSessionRepository
                .findByStripeCheckoutSessionIdForUpdate(expiredEvent.getStripeCheckoutSessionId())
                .orElse(null);

        if (checkoutSession == null) {
            logger.info("event=checkout_session_expiry_ignored reason=checkout_session_not_found provider=stripe");
            return;
        }

        if (checkoutSession.getStatus() == CheckoutSessionStatus.FINALIZED) {
            logger.info(
                    "event=checkout_session_expiry_ignored checkoutSessionId={} reason=already_finalized",
                    checkoutSession.getId()
            );
            return;
        }

        if (checkoutSession.getStatus() == CheckoutSessionStatus.CANCELLED
                || checkoutSession.getStatus() == CheckoutSessionStatus.EXPIRED
                || checkoutSession.getStatus() == CheckoutSessionStatus.REFUND_PENDING
                || checkoutSession.getStatus() == CheckoutSessionStatus.REFUNDED) {
            logger.info(
                    "event=checkout_session_expiry_ignored checkoutSessionId={} reason=terminal_status status={}",
                    checkoutSession.getId(),
                    checkoutSession.getStatus()
            );
            return;
        }

        expireActiveLocksForCheckoutSession(checkoutSession);
        expireAuditLocksForCheckoutSession(checkoutSession);
        redisSeatMapCacheService.evict(checkoutSession.getShowtime().getId());

        checkoutSession.setStatus(CheckoutSessionStatus.EXPIRED);

        checkoutSessionRepository.save(checkoutSession);
        outboxEventService.recordCheckoutSessionExpired(checkoutSession);
        logger.info(
                "event=checkout_session_expired checkoutSessionId={} showtimeId={}",
                checkoutSession.getId(),
                checkoutSession.getShowtime().getId()
        );
    }

    private void expireActiveLocksForCheckoutSession(CheckoutSession checkoutSession) {
        List<Seat> seats = loadSeatsFromCheckoutSnapshot(checkoutSession);
        RedisSeatLockOwner owner = buildRedisOwner(
                checkoutSession.getUser(),
                checkoutSession.getGuestSessionId(),
                checkoutSession.getGuestEmail()
        );

        redisSeatLockService.releaseLocks(
                checkoutSession.getShowtime().getId(),
                seats.stream().map(Seat::getId).toList(),
                owner
        );
    }

    private void updateAuditLocksToConverted(CheckoutSession checkoutSession, List<Seat> seats) {
        for (Seat seat : seats) {
            seatLockRepository.markActiveLockAsConverted(
                    checkoutSession.getShowtime().getId(),
                    seat.getId(),
                    checkoutSession.getUser() != null ? checkoutSession.getUser().getId() : null,
                    checkoutSession.getGuestSessionId(),
                    checkoutSession.getGuestEmail()
            );
        }
    }

    private void expireAuditLocksForCheckoutSession(CheckoutSession checkoutSession) {
        List<Seat> seats = loadSeatsFromCheckoutSnapshot(checkoutSession);

        for (Seat seat : seats) {
            seatLockRepository.cancelActiveLock(
                    checkoutSession.getShowtime().getId(),
                    seat.getId(),
                    checkoutSession.getUser() != null ? checkoutSession.getUser().getId() : null,
                    checkoutSession.getGuestSessionId(),
                    checkoutSession.getGuestEmail()
            );
        }
    }

    private RedisSeatLockOwner buildRedisOwner(User user, String sessionId, String guestEmail) {
        if (user != null) {
            return redisSeatLockService.authenticatedOwner(user.getId());
        }

        return redisSeatLockService.guestOwner(sessionId, guestEmail);
    }

}
