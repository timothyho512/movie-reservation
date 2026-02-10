package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.ShowtimeRequest;
import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.ShowtimeStatus;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShowtimeService {
    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ScreenRepository screenRepository;

    public List<Showtime> getAllShowtimes() {
        return showtimeRepository.findAll();
    }

    public Showtime getShowtimeById(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + id));
    }

    public Showtime createShowtime(ShowtimeRequest request) {
        // Find the movie by ID
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));

        // Find the screen by ID
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + request.getScreenId()));

        // Create showtime with both relationships
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getEndTime());
        showtime.setBasePrice(request.getBasePrice());
        showtime.setTotalSeats(screen.getTotalSeats());
        showtime.setAvailableSeats(screen.getTotalSeats());
        showtime.setStatus(request.getStatus() != null ? request.getStatus() : ShowtimeStatus.UPCOMING);

        return showtimeRepository.save(showtime);
    }

    public Showtime updateShowtime(Long id, ShowtimeRequest request) {
        Showtime showtime = getShowtimeById(id);

        // Update movie if provided
        if (request.getMovieId() != null) {
            Movie movie = movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));
            showtime.setMovie(movie);
        }

        // Update screen if provided
        if (request.getScreenId() != null) {
            Screen screen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + request.getScreenId()));
            showtime.setScreen(screen);
            showtime.setTotalSeats(screen.getTotalSeats());
        }

        // Update other fields if provided
        if (request.getStartTime() != null) {
            showtime.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            showtime.setEndTime(request.getEndTime());
        }
        if (request.getBasePrice() != null) {
            showtime.setBasePrice(request.getBasePrice());
        }
        if (request.getStatus() != null) {
            showtime.setStatus(request.getStatus());
        }

        return showtimeRepository.save(showtime);
    }

    public void deleteShowtime(Long id) {
        Showtime showtime = getShowtimeById(id);
        showtimeRepository.delete(showtime);
    }
}
