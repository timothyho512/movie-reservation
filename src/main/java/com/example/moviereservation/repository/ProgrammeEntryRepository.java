package com.example.moviereservation.repository;

import com.example.moviereservation.entity.ProgrammeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProgrammeEntryRepository extends JpaRepository<ProgrammeEntry, Long> {
    @Query("""
            SELECT pe
            FROM ProgrammeEntry pe
            JOIN FETCH pe.movie m
            WHERE pe.startsOn = :startsOn
            ORDER BY m.title ASC
            """)
    List<ProgrammeEntry> findProgrammeStartingOn(@Param("startsOn") LocalDate startsOn);

    Optional<ProgrammeEntry> findTopByStartsOnBeforeOrderByStartsOnDesc(LocalDate startsOn);

    List<ProgrammeEntry> findAllByMovieIdOrderByStartsOnDesc(Long movieId);

    @Query("SELECT DISTINCT pe.movie.id FROM ProgrammeEntry pe")
    List<Long> findAllProgrammedMovieIds();
}
