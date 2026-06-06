package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findAllByActiveTrueOrderByTitleAsc();
}
