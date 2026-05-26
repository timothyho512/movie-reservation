package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            JOIN r.seats s
            WHERE r.showtime.id = :showtimeId
                AND s.id = :seatId
                AND r.status IN (
                    com.example.moviereservation.entity.ReservationStatus.PENDING,
                    com.example.moviereservation.entity.ReservationStatus.CONFIRMED,
                    com.example.moviereservation.entity.ReservationStatus.COMPLETED
                )
            """)    
    boolean existsReservedSeatForShowtime(@Param("showtimeId") Long showtimeId, @Param("seatId") Long seatId);

    @Query("""
        SELECT s.id
        FROM Reservation r
        JOIN r.seats s
        WHERE r.showtime.id = :showtimeId
        AND r.status IN (
            com.example.moviereservation.entity.ReservationStatus.PENDING,
            com.example.moviereservation.entity.ReservationStatus.CONFIRMED,
            com.example.moviereservation.entity.ReservationStatus.COMPLETED
        )
    """)
    List<Long> findReservedSeatIdsForShowtime(@Param("showtimeId") Long showtimeId);

    List<Reservation> findAllByUserIdOrderByBookingTimeDesc(Long userId);

    Optional<Reservation> findByBookingReference(String bookingReference);
}
