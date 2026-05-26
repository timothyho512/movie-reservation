package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findAllByOrderByStartTimeAsc();

    List<Showtime> findAllByMovieIdOrderByStartTimeAsc(Long movieId);
}
