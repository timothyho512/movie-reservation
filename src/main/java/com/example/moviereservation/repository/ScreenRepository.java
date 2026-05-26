package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findAllByTheatreIdOrderByNameAsc(Long theatreId);
}
