package com.example.moviereservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.moviereservation.entity.SeatLock;

public interface SeatLockRepository extends JpaRepository<SeatLock, Long>{

    @Query("""
        SELECT CASE WHEN COUNT(sl) > 0 THEN true ELSE false END
        FROM SeatLock sl
        WHERE sl.showtime.id = :showtimeId
        AND sl.seat.id = :seatId
        AND sl.status = com.example.moviereservation.entity.LockStatus.LOCKED
        AND sl.expiresAt > CURRENT_TIMESTAMP
        AND (
            (:userId IS NOT NULL AND sl.user.id = :userId)
            OR
            (:sessionId IS NOT NULL AND sl.sessionId = :sessionId AND sl.guestEmail = :guestEmail)
        )
    """)
    boolean existsActiveLockForOwner(
            @Param("showtimeId") Long showtimeId,
            @Param("seatId") Long seatId,
            @Param("userId") Long userId,
            @Param("sessionId") String sessionId,
            @Param("guestEmail") String guestEmail
    );
    
    @Modifying
    @Query("""
        UPDATE SeatLock sl
        SET sl.status = com.example.moviereservation.entity.LockStatus.CONVERTED_TO_RESERVATION
        WHERE sl.showtime.id = :showtimeId
        AND sl.seat.id = :seatId
        AND sl.status = com.example.moviereservation.entity.LockStatus.LOCKED
        AND sl.expiresAt > CURRENT_TIMESTAMP
        AND (
            (:userId IS NOT NULL AND sl.user.id = :userId)
            OR
            (:sessionId IS NOT NULL AND sl.sessionId = :sessionId AND sl.guestEmail = :guestEmail)
        )
    """)
    int markActiveLockAsConverted(
            @Param("showtimeId") Long showtimeId,
            @Param("seatId") Long seatId,
            @Param("userId") Long userId,
            @Param("sessionId") String sessionId,
            @Param("guestEmail") String guestEmail
    );

    @Modifying
    @Query("""
        UPDATE SeatLock sl
        SET sl.status = com.example.moviereservation.entity.LockStatus.EXPIRED
        WHERE sl.status = com.example.moviereservation.entity.LockStatus.LOCKED
        AND sl.expiresAt < CURRENT_TIMESTAMP
    """)
    int expireTimedOutLocks();

    @Modifying
    @Query("""
        UPDATE SeatLock sl
        SET sl.status = com.example.moviereservation.entity.LockStatus.EXPIRED
        WHERE sl.showtime.id = :showtimeId
        AND sl.status = com.example.moviereservation.entity.LockStatus.LOCKED
        AND sl.expiresAt > CURRENT_TIMESTAMP
        AND sl.seat.id = :seatId
        AND (
            (:userId IS NOT NULL AND sl.user.id = :userId)
            OR
            (:sessionId IS NOT NULL AND sl.sessionId = :sessionId AND sl.guestEmail = :guestEmail)
        )
    """)
    int cancelActiveLock(
            @Param("showtimeId") Long showtimeId,
            @Param("seatId") Long seatId,
            @Param("userId") Long userId,
            @Param("sessionId") String sessionId,
            @Param("guestEmail") String guestEmail
    );

    @Modifying
    @Query("""
        UPDATE SeatLock sl
        SET sl.status = com.example.moviereservation.entity.LockStatus.EXPIRED
        WHERE sl.showtime.id = :showtimeId
          AND sl.status = com.example.moviereservation.entity.LockStatus.LOCKED
    """)
    int expireAllActiveLocksForShowtime(@Param("showtimeId") Long showtimeId);
}
