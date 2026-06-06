package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findAllByTheatreIdOrderByNameAsc(Long theatreId);

    long countByTheatreIdAndActiveTrue(Long theatreId);

    @Modifying
    @Transactional
    @Query("UPDATE Screen s SET s.currentLayoutVersion = null")
    int clearCurrentLayoutVersions();
}
