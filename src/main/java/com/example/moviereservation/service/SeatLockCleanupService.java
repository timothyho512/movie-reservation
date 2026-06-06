package com.example.moviereservation.service;

import com.example.moviereservation.repository.CheckoutSessionRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.entity.CheckoutSession;
import com.example.moviereservation.entity.CheckoutSessionStatus;
import com.example.moviereservation.entity.Seat;
import com.example.moviereservation.entity.CheckoutItemSnapshot;
import com.example.moviereservation.repository.SeatRepository;
import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SeatLockCleanupService {
    
    private final SeatLockRepository seatLockRepository;

    private final CheckoutSessionRepository checkoutSessionRepository;

    private final OutboxEventService outboxEventService;
    private final StripeCheckoutService stripeCheckoutService;
    private final RedisSeatLockService redisSeatLockService;
    private final RedisSeatMapCacheService redisSeatMapCacheService;
    private final SeatRepository seatRepository;
    private final ObjectMapper objectMapper;
    private final CheckoutSessionService checkoutSessionService;

    public SeatLockCleanupService(
            SeatLockRepository seatLockRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            OutboxEventService outboxEventService,
            StripeCheckoutService stripeCheckoutService,
            RedisSeatLockService redisSeatLockService,
            RedisSeatMapCacheService redisSeatMapCacheService,
            SeatRepository seatRepository,
            ObjectMapper objectMapper,
            CheckoutSessionService checkoutSessionService
    ) {
        this.seatLockRepository = seatLockRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.outboxEventService = outboxEventService;
        this.stripeCheckoutService = stripeCheckoutService;
        this.redisSeatLockService = redisSeatLockService;
        this.redisSeatMapCacheService = redisSeatMapCacheService;
        this.seatRepository = seatRepository;
        this.objectMapper = objectMapper;
        this.checkoutSessionService = checkoutSessionService;
    }

    @Transactional
    public int expireTimedOutLocks() {
        return seatLockRepository.expireTimedOutLocks();
    }

    @Transactional
    public int expireStalePendingCheckoutSessions() {
        List<CheckoutSession> checkoutSessions = checkoutSessionRepository.findAllByStatusAndExpiresAtBefore(
                CheckoutSessionStatus.PENDING_PAYMENT,
                LocalDateTime.now()
        );

        checkoutSessions.forEach(this::expireCheckoutSession);

        return checkoutSessions.size();
    }

    @Transactional
    public int expirePendingCheckoutSessionsForShowtime(Long showtimeId) {
        List<CheckoutSession> checkoutSessions =
                checkoutSessionRepository.findAllByShowtimeIdAndStatus(
                        showtimeId,
                        CheckoutSessionStatus.PENDING_PAYMENT
                );
        checkoutSessions.forEach(this::expireCheckoutSession);
        return checkoutSessions.size();
    }

    @Transactional
    public int retryPendingRefunds() {
        List<CheckoutSession> sessions =
                checkoutSessionRepository.findAllByStatus(CheckoutSessionStatus.REFUND_PENDING);
        sessions.forEach(checkoutSessionService::retryRefund);
        return sessions.size();
    }

    private void releaseActiveLocks(CheckoutSession checkoutSession) {
        List<Seat> seats = loadSeats(checkoutSession);
        RedisSeatLockService.RedisSeatLockOwner owner = checkoutSession.getUser() != null
                ? redisSeatLockService.authenticatedOwner(checkoutSession.getUser().getId())
                : redisSeatLockService.guestOwner(
                        checkoutSession.getGuestSessionId(),
                        checkoutSession.getGuestEmail()
                );
        redisSeatLockService.releaseLocks(
                checkoutSession.getShowtime().getId(),
                seats.stream().map(Seat::getId).toList(),
                owner
        );
    }

    private void expireCheckoutSession(CheckoutSession checkoutSession) {
        try {
            stripeCheckoutService.expireHostedCheckoutSession(checkoutSession.getStripeCheckoutSessionId());
        } catch (RuntimeException ignored) {
            // Stripe may already have completed or expired the session. Local expiry still proceeds.
        }

        releaseActiveLocks(checkoutSession);
        expireAuditLocks(checkoutSession);
        redisSeatMapCacheService.evict(checkoutSession.getShowtime().getId());
        checkoutSession.setStatus(CheckoutSessionStatus.EXPIRED);
        checkoutSessionRepository.save(checkoutSession);
        outboxEventService.recordCheckoutSessionExpired(checkoutSession);
    }

    private void expireAuditLocks(CheckoutSession checkoutSession) {
        for (Seat seat : loadSeats(checkoutSession)) {
            seatLockRepository.cancelActiveLock(
                    checkoutSession.getShowtime().getId(),
                    seat.getId(),
                    checkoutSession.getUser() != null ? checkoutSession.getUser().getId() : null,
                    checkoutSession.getGuestSessionId(),
                    checkoutSession.getGuestEmail()
            );
        }
    }

    private List<Seat> loadSeats(CheckoutSession checkoutSession) {
        try {
            CheckoutItemSnapshot[] items =
                    objectMapper.readValue(checkoutSession.getItemsSnapshotJson(), CheckoutItemSnapshot[].class);
            return Arrays.stream(items)
                    .map(item -> seatRepository.findById(item.getSeatId())
                            .orElseThrow(() -> new IllegalStateException("Checkout seat not found: " + item.getSeatId())))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load checkout seats for expiry", e);
        }
    }
}
