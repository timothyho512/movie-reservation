package com.example.moviereservation.service;

import com.example.moviereservation.repository.CheckoutSessionRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

@Service
public class SeatLockCleanupService {
    
    private final SeatLockRepository seatLockRepository;

    private final CheckoutSessionRepository checkoutSessionRepository;

    public SeatLockCleanupService(SeatLockRepository seatLockRepository, CheckoutSessionRepository checkoutSessionRepository) {
        this.seatLockRepository = seatLockRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
    }

    @Transactional
    public int expireTimedOutLocks() {
        return seatLockRepository.expireTimedOutLocks();
    }

    @Transactional
    public int expireStalePendingCheckoutSessions() {
        return checkoutSessionRepository.expireStalePendingSessions(LocalDateTime.now());
    }
}
