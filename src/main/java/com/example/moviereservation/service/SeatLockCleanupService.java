package com.example.moviereservation.service;

import com.example.moviereservation.repository.CheckoutSessionRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.entity.CheckoutSession;
import com.example.moviereservation.entity.CheckoutSessionStatus;
import com.example.moviereservation.service.OutboxEventService;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SeatLockCleanupService {
    
    private final SeatLockRepository seatLockRepository;

    private final CheckoutSessionRepository checkoutSessionRepository;

    private final OutboxEventService outboxEventService;

    public SeatLockCleanupService(
            SeatLockRepository seatLockRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            OutboxEventService outboxEventService
    ) {
        this.seatLockRepository = seatLockRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.outboxEventService = outboxEventService;
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

        for (CheckoutSession checkoutSession : checkoutSessions) {
            checkoutSession.setStatus(CheckoutSessionStatus.EXPIRED);
            checkoutSessionRepository.save(checkoutSession);
            outboxEventService.recordCheckoutSessionExpired(checkoutSession);
        }

        return checkoutSessions.size();
    }
}
