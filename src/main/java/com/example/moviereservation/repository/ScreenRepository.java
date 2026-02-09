package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, Long> {}
