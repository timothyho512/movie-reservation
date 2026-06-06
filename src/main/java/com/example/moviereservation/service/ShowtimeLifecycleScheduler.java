package com.example.moviereservation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ShowtimeLifecycleScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ShowtimeLifecycleScheduler.class);

    private final ShowtimeLifecycleService showtimeLifecycleService;

    public ShowtimeLifecycleScheduler(ShowtimeLifecycleService showtimeLifecycleService) {
        this.showtimeLifecycleService = showtimeLifecycleService;
    }

    @Scheduled(fixedRate = 60000)
    public void synchronizeShowtimes() {
        ShowtimeLifecycleService.LifecycleResult result =
                showtimeLifecycleService.synchronize(LocalDateTime.now());

        if (result.ongoingShowtimes() > 0
                || result.completedShowtimes() > 0
                || result.completedReservations() > 0) {
            logger.info(
                    "event=showtime_lifecycle_synchronized ongoing={} completed={} reservationsCompleted={}",
                    result.ongoingShowtimes(),
                    result.completedShowtimes(),
                    result.completedReservations()
            );
        }
    }
}
