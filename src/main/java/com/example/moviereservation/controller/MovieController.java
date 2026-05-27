package com.example.moviereservation.controller;

import com.example.moviereservation.dto.MovieCardResponse;
import com.example.moviereservation.dto.MovieDetailResponse;
import com.example.moviereservation.dto.MovieRequest;
import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    @Autowired
    private MovieService movieService;

    // GET /api/movies - Get all movies
    @GetMapping
    public ResponseEntity<List<MovieCardResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getMovieCards());
    }

    // Get /api/movies/{id} - Get movie by ID
    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieDetail(id));
    }

    // POST /api/movies - Create new movie
    @PostMapping
    public ResponseEntity<MovieDetailResponse> createMovie(@RequestBody MovieRequest request) {
        Movie savedMovie = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.getMovieDetail(savedMovie.getId()));
    }

    // PUT /api/movies/{id} - Update movie
    @PutMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> updateMovie(@PathVariable Long id, @RequestBody MovieRequest request) {
        Movie updatedMovie = movieService.updateMovie(id, request);
        return ResponseEntity.ok(movieService.getMovieDetail(updatedMovie.getId()));
    }

    // DELETE /api/movies/{id} - Delete movie
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
