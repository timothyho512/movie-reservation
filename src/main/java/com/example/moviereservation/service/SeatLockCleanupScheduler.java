package com.example.moviereservation.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class SeatLockCleanupScheduler {
    
    private final SeatLockCleanupService seatLockCleanupService;

    public SeatLockCleanupScheduler(SeatLockCleanupService seatLockCleanupService) {
        this.seatLockCleanupService = seatLockCleanupService;
    }

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void expireOldLocks() {
        int expiredCount = seatLockCleanupService.expireTimedOutLocks();

        if (expiredCount > 0) {
            System.out.println("Expired " + expiredCount + " seat locks");
        }
    }
}
