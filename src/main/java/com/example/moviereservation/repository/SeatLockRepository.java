package com.example.moviereservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.moviereservation.entity.SeatLock;

public interface SeatLockRepository extends JpaRepository<SeatLock, Long>{

    @Query("""
            SELECT CASE WHEN COUNT(sl) > 0 THEN true ELSE false END
            FROM SeatLock sl
            WHERE sl.seat.id = :seatId
                AND sl.showtime.id = :showtimeId
                AND sl.expiresAt > CURRENT_TIMESTAMP
                AND sl.status IN (
                    com.example.moviereservation.entity.LockStatus.LOCKED,
                    com.example.moviereservation.entity.LockStatus.CONVERTED_TO_RESERVATION
                )
            """)
    boolean existsLockedSeatForShowtime(@Param("showtimeId") Long showtimeId, @Param("seatId") Long seatId);
}
