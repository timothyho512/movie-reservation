package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long>, JpaSpecificationExecutor<Screen> {
    List<Screen> findAllByTheatreIdOrderByNameAsc(Long theatreId);

    long countByTheatreIdAndActiveTrue(Long theatreId);

    @Query("""
            SELECT s
            FROM Screen s
            JOIN FETCH s.theatre t
            LEFT JOIN FETCH s.currentLayoutVersion
            WHERE s.active = true
              AND t.active = true
              AND s.currentLayoutVersion IS NOT NULL
            ORDER BY t.name ASC, s.name ASC
            """)
    List<Screen> findActiveSchedulingScreens();

    @Modifying
    @Transactional
    @Query("UPDATE Screen s SET s.currentLayoutVersion = null")
    int clearCurrentLayoutVersions();
}
