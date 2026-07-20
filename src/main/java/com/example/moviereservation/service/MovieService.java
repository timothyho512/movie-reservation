package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.MovieCardResponse;
import com.example.moviereservation.dto.MovieDetailResponse;
import com.example.moviereservation.dto.MovieRequest;
import com.example.moviereservation.dto.ShowtimeSummaryResponse;
import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class MovieService {
    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private BookingWindowService bookingWindowService;

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    public List<MovieCardResponse> getMovieCards() {
        return showtimeRepository.findMoviesWithBookableShowtimes(
                        bookingWindowService.bookingCutoffFrom(LocalDateTime.now())
                ).stream()
                .map(this::toMovieCardResponse)
                .toList();
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
    }

    public MovieDetailResponse getMovieDetail(Long id) {
        Movie movie = getMovieById(id);
        List<ShowtimeSummaryResponse> showtimes = showtimeRepository.findBookableShowtimesByMovieId(
                        id,
                        bookingWindowService.bookingCutoffFrom(LocalDateTime.now())
                ).stream()
                .map(this::toShowtimeSummaryResponse)
                .toList();

        return new MovieDetailResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getPosterPath(),
                movie.getOverview(),
                movie.getRuntimeMinutes(),
                showtimes
        );
    }

    public Movie createMovie(MovieRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDirector(request.getDirector());
        movie.setActive(true);
        return movieRepository.save(movie);
    }

    public Movie updateMovie(Long id, MovieRequest request) {
        Movie movie = getMovieById(id);

        if (request.getTitle() != null) {
            movie.setTitle(request.getTitle());
        }
        if (request.getDirector() != null) {
            movie.setDirector(request.getDirector());
        }
        return movieRepository.save(movie);
    }

    public void deleteMovie(Long id) {
        Movie movie = getMovieById(id);
        if (showtimeRepository.existsFutureShowtimeForMovie(
                id,
                LocalDateTime.now()
        )) {
            throw new IllegalArgumentException("Cannot deactivate a movie with future showtimes");
        }
        movie.setActive(false);
        movieRepository.save(movie);
    }

    private MovieCardResponse toMovieCardResponse(Movie movie) {
        return new MovieCardResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getPosterPath()
        );
    }

    private ShowtimeSummaryResponse toShowtimeSummaryResponse(Showtime showtime) {
        Movie movie = showtime.getMovie();
        Screen screen = showtime.getScreen();
        Theatre theatre = screen.getTheatre();

        return new ShowtimeSummaryResponse(
                showtime.getId(),
                new ShowtimeSummaryResponse.MovieSummary(
                        movie.getId(),
                        movie.getTitle(),
                        movie.getDirector()
                ),
                new ShowtimeSummaryResponse.TheatreSummary(
                        theatre.getId(),
                        theatre.getName(),
                        theatre.getCity(),
                        theatre.getCountry()
                ),
                new ShowtimeSummaryResponse.ScreenSummary(
                        screen.getId(),
                        screen.getName(),
                        screen.getScreenType()
                ),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getBasePrice(),
                showtime.getAvailableSeats(),
                showtime.getTotalSeats(),
                showtime.getStatus()
        );
    }
}
