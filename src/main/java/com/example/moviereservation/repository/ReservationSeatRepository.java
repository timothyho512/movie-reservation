package com.example.moviereservation.repository;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {}