package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    List<Movie> findAllByActiveTrueOrderByTitleAsc();
    List<Movie> findAllByActiveTrueAndNowPlayingTrueOrderByTitleAsc();
    List<Movie> findAllByTmdbManagedTrue();
    Optional<Movie> findByTmdbId(Long tmdbId);
    Optional<Movie> findByTitle(String title);
}
