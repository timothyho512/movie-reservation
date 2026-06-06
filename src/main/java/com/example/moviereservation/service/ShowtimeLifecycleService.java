package com.example.moviereservation.service;

import com.example.moviereservation.repository.ReservationRepository;
import com.example.moviereservation.repository.SeatLockRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ShowtimeLifecycleService {
    private final ShowtimeRepository showtimeRepository;
    private final ReservationRepository reservationRepository;
    private final SeatLockCleanupService seatLockCleanupService;
    private final SeatLockRepository seatLockRepository;
    private final RedisSeatLockService redisSeatLockService;
    private final RedisSeatMapCacheService redisSeatMapCacheService;

    public ShowtimeLifecycleService(
            ShowtimeRepository showtimeRepository,
            ReservationRepository reservationRepository,
            SeatLockCleanupService seatLockCleanupService,
            SeatLockRepository seatLockRepository,
            RedisSeatLockService redisSeatLockService,
            RedisSeatMapCacheService redisSeatMapCacheService
    ) {
        this.showtimeRepository = showtimeRepository;
        this.reservationRepository = reservationRepository;
        this.seatLockCleanupService = seatLockCleanupService;
        this.seatLockRepository = seatLockRepository;
        this.redisSeatLockService = redisSeatLockService;
        this.redisSeatMapCacheService = redisSeatMapCacheService;
    }

    @Transactional
    public LifecycleResult synchronize(LocalDateTime now) {
        List<Long> startingIds = showtimeRepository.findIdsReadyToStart(now);
        List<Long> completedIds = showtimeRepository.findIdsReadyToComplete(now);

        int ongoingCount = startingIds.isEmpty() ? 0 : showtimeRepository.markOngoing(startingIds);
        int completedCount = completedIds.isEmpty() ? 0 : showtimeRepository.markCompleted(completedIds);
        int completedReservationCount = completedIds.isEmpty()
                ? 0
                : reservationRepository.completePaidConfirmedReservations(completedIds);

        Set<Long> closedBookingIds = new LinkedHashSet<>(startingIds);
        closedBookingIds.addAll(completedIds);
        for (Long showtimeId : closedBookingIds) {
            seatLockCleanupService.expirePendingCheckoutSessionsForShowtime(showtimeId);
            redisSeatLockService.releaseAllLocksForShowtime(showtimeId);
            seatLockRepository.expireAllActiveLocksForShowtime(showtimeId);
            redisSeatMapCacheService.evict(showtimeId);
        }

        return new LifecycleResult(ongoingCount, completedCount, completedReservationCount);
    }

    public record LifecycleResult(
            int ongoingShowtimes,
            int completedShowtimes,
            int completedReservations
    ) {
    }
}
