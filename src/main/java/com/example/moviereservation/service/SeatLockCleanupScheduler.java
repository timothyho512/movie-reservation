package com.example.moviereservation.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.moviereservation.observability.CheckoutMetrics;


@Component
public class SeatLockCleanupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(SeatLockCleanupScheduler.class);
    
    private final SeatLockCleanupService seatLockCleanupService;

    private final CheckoutMetrics checkoutMetrics;

    public SeatLockCleanupScheduler(
            SeatLockCleanupService seatLockCleanupService,
            CheckoutMetrics checkoutMetrics
    ) {
        this.seatLockCleanupService = seatLockCleanupService;
        this.checkoutMetrics = checkoutMetrics;
    }

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void expireOldLocks() {
        int expiredLockCount = seatLockCleanupService.expireTimedOutLocks();
        int expiredCheckoutSessionCount = seatLockCleanupService.expireStalePendingCheckoutSessions();
        seatLockCleanupService.retryPendingRefunds();

        if (expiredLockCount > 0) {
            checkoutMetrics.recordExpiredLocks(expiredLockCount);
            logger.info("event=seat_lock_cleanup_expired_locks count={}", expiredLockCount);
        }

        if (expiredCheckoutSessionCount > 0) {
            checkoutMetrics.recordExpiredCheckoutSessions(expiredCheckoutSessionCount);
            logger.info("event=seat_lock_cleanup_expired_checkout_sessions count={}", expiredCheckoutSessionCount);
        }
    }
}
