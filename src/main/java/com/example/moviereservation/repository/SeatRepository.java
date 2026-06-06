package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId")
    List<Seat> findByScreenId(@Param("screenId") Long screenId);

    @Query("""
            SELECT s
            FROM Seat s
            WHERE s.screen.id = :screenId
            AND s.active = true
            AND (
                (:layoutVersionId IS NOT NULL AND s.layoutVersion.id = :layoutVersionId)
                OR (:layoutVersionId IS NULL AND s.layoutVersion IS NULL)
            )
            """)
    List<Seat> findActiveByScreenIdAndLayoutVersionId(
            @Param("screenId") Long screenId,
            @Param("layoutVersionId") Long layoutVersionId
    );

    long countByScreenIdAndLayoutVersionIdAndActiveTrue(Long screenId, Long layoutVersionId);

    @Query("""
            SELECT COUNT(s)
            FROM Seat s
            WHERE s.screen.theatre.id = :theatreId
            AND s.screen.active = true
            AND s.active = true
            AND s.layoutVersion = s.screen.currentLayoutVersion
            """)
    long countActiveCurrentLayoutSeatsByTheatreId(@Param("theatreId") Long theatreId);
}
