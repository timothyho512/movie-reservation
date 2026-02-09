package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {}
