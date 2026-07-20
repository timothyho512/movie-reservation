package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {
    List<Showtime> findAllByOrderByStartTimeAsc();

    List<Showtime> findAllByMovieIdOrderByStartTimeAsc(Long movieId);

    List<Showtime> findAllByScreenId(Long screenId);

    @Query("""
            SELECT DISTINCT m
            FROM Showtime st
            JOIN st.movie m
            JOIN st.screen sc
            JOIN sc.theatre t
            WHERE st.status = com.example.moviereservation.entity.ShowtimeStatus.UPCOMING
              AND st.startTime > :bookingCutoff
              AND m.active = true
              AND sc.active = true
              AND t.active = true
            ORDER BY m.title ASC
            """)
    List<Movie> findMoviesWithBookableShowtimes(@Param("bookingCutoff") LocalDateTime bookingCutoff);

    @Query("""
            SELECT CASE WHEN COUNT(st) > 0 THEN true ELSE false END
            FROM Showtime st
            WHERE st.movie.id = :movieId
              AND st.status = com.example.moviereservation.entity.ShowtimeStatus.UPCOMING
              AND st.startTime > :bookingCutoff
            """)
    boolean existsFutureShowtimeForMovie(
            @Param("movieId") Long movieId,
            @Param("bookingCutoff") LocalDateTime bookingCutoff
    );

    @Query("""
            SELECT CASE WHEN COUNT(st) > 0 THEN true ELSE false END
            FROM Showtime st
            WHERE st.screen.id = :screenId
              AND st.status <> com.example.moviereservation.entity.ShowtimeStatus.CANCELLED
              AND st.startTime < :endTime
              AND st.endTime > :startTime
            """)
    boolean existsOverlappingShowtime(
            @Param("screenId") Long screenId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
            SELECT st
            FROM Showtime st
            JOIN FETCH st.movie m
            JOIN FETCH st.screen sc
            JOIN FETCH sc.theatre t
            WHERE st.status = com.example.moviereservation.entity.ShowtimeStatus.UPCOMING
              AND st.startTime > :bookingCutoff
              AND m.active = true
              AND sc.active = true
              AND t.active = true
            ORDER BY st.startTime ASC
            """)
    List<Showtime> findBookableShowtimes(@Param("bookingCutoff") LocalDateTime bookingCutoff);

    @Query("""
            SELECT st
            FROM Showtime st
            JOIN FETCH st.movie m
            JOIN FETCH st.screen sc
            JOIN FETCH sc.theatre t
            WHERE st.movie.id = :movieId
              AND st.status = com.example.moviereservation.entity.ShowtimeStatus.UPCOMING
              AND st.startTime > :bookingCutoff
              AND m.active = true
              AND sc.active = true
              AND t.active = true
            ORDER BY st.startTime ASC
            """)
    List<Showtime> findBookableShowtimesByMovieId(
            @Param("movieId") Long movieId,
            @Param("bookingCutoff") LocalDateTime bookingCutoff
    );

    @Query("""
            SELECT st.id
            FROM Showtime st
            WHERE st.status = com.example.moviereservation.entity.ShowtimeStatus.UPCOMING
              AND st.startTime <= :now
              AND st.endTime > :now
            """)
    List<Long> findIdsReadyToStart(@Param("now") LocalDateTime now);

    @Query("""
            SELECT st.id
            FROM Showtime st
            WHERE st.status IN (
                com.example.moviereservation.entity.ShowtimeStatus.UPCOMING,
                com.example.moviereservation.entity.ShowtimeStatus.ONGOING
            )
              AND st.endTime <= :now
            """)
    List<Long> findIdsReadyToComplete(@Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE Showtime st
            SET st.status = com.example.moviereservation.entity.ShowtimeStatus.ONGOING
            WHERE st.id IN :ids
              AND st.status = com.example.moviereservation.entity.ShowtimeStatus.UPCOMING
            """)
    int markOngoing(@Param("ids") List<Long> ids);

    @Modifying
    @Query("""
            UPDATE Showtime st
            SET st.status = com.example.moviereservation.entity.ShowtimeStatus.COMPLETED
            WHERE st.id IN :ids
              AND st.status IN (
                  com.example.moviereservation.entity.ShowtimeStatus.UPCOMING,
                  com.example.moviereservation.entity.ShowtimeStatus.ONGOING
              )
            """)
    int markCompleted(@Param("ids") List<Long> ids);
}
