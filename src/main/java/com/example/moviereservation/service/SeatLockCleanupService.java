package com.example.moviereservation.service;

import com.example.moviereservation.repository.SeatLockRepository;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

@Service
public class SeatLockCleanupService {
    
    private final SeatLockRepository seatLockRepository;

    public SeatLockCleanupService(SeatLockRepository seatLockRepository) {
        this.seatLockRepository = seatLockRepository;
    }

    @Transactional
    public int expireTimedOutLocks() {
        return seatLockRepository.expireTimedOutLocks();
    }
}
