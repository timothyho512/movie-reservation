package com.example.moviereservation.service;

import com.example.moviereservation.Exception.ResourceNotFoundException;
import com.example.moviereservation.dto.MovieRequest;
import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieServiceTest {
    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private BookingWindowService bookingWindowService;

    @InjectMocks
    private MovieService movieService;

    private Movie testMovie;

    @BeforeEach
    public void setUp() {
        testMovie = new Movie();
        testMovie.setId(1L);
        testMovie.setTitle("Inception");
        testMovie.setDirector("Christopher Nolan");
    }

    @Test
    public void getAllMovies_ReturnListOfMovies() {
        Movie movie2 = new Movie();
        movie2.setId(2L);
        movie2.setTitle("Interstellar");
        movie2.setDirector("Christopher Nolan");

        List<Movie> expectedMovies = Arrays.asList(testMovie, movie2);
        when(movieRepository.findAll()).thenReturn(expectedMovies);

        List<Movie> actualMovie = movieService.getAllMovies();

        assertEquals(2, actualMovie.size());
        assertEquals(expectedMovies, actualMovie);
        verify(movieRepository, times(1)).findAll();
    }

    @Test
    void getAllMovies_ReturnsEmptyList_WhenNoMovies() {
        when(movieRepository.findAll()).thenReturn(Arrays.asList());

        List<Movie> actualMovies = movieService.getAllMovies();

        assertTrue(actualMovies.isEmpty());
        verify(movieRepository, times(1)).findAll();
    }

    @Test
    void getMovieCards_ReturnsOnlyMoviesWithBookableShowtimes() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 7, 20, 10, 10);
        when(bookingWindowService.bookingCutoffFrom(any(LocalDateTime.class))).thenReturn(cutoff);
        when(showtimeRepository.findMoviesWithBookableShowtimes(cutoff)).thenReturn(List.of(testMovie));

        var result = movieService.getMovieCards();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Inception");
        verify(showtimeRepository).findMoviesWithBookableShowtimes(cutoff);
    }

    @Test
    void getMovieById_MovieExists_ReturnsMovie() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));

        Movie result = movieService.getMovieById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Inception", result.getTitle());
        assertEquals("Christopher Nolan", result.getDirector());
        verify(movieRepository, times(1)).findById(1L);
    }

    @Test
    void getMovieById_MovieNotFound_ThrowsException() {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            movieService.getMovieById(999L);
        });

        assertTrue(exception.getMessage().contains("not found"));
        verify(movieRepository, times(1)).findById(999L);
    }

    @Test
    void createMovie_Success() {
        MovieRequest request = new MovieRequest();
        request.setTitle("Inception");
        request.setDirector("Christopher Nolan");

        when(movieRepository.save(any(Movie.class))).thenReturn(testMovie);

        Movie result = movieService.createMovie(request);

        assertNotNull(result);
        assertEquals("Inception", result.getTitle());
        assertEquals("Christopher Nolan", result.getDirector());
        verify(movieRepository, times(1)).save(any(Movie.class));
    }

    @Test
    void createMovie_WithNullTitle_StillSaves() {
        MovieRequest request = new MovieRequest();
        request.setTitle(null);
        request.setDirector("Christopher Nolan");

        Movie movieWithNullTitle = new Movie();
        movieWithNullTitle.setId(1L);
        movieWithNullTitle.setTitle(null);
        movieWithNullTitle.setDirector("Christopher Nolan");

        when(movieRepository.save(any(Movie.class))).thenReturn(movieWithNullTitle);

        Movie result = movieService.createMovie(request);

        assertNotNull(result);
        assertNull(result.getTitle());
        assertEquals("Christopher Nolan", result.getDirector());
        verify(movieRepository, times(1)).save(any(Movie.class));
    }

    @Test
    void updateMovie_Success() {
        MovieRequest request = new MovieRequest();
        request.setTitle("Inception Updated");
        request.setDirector("Chris Nolan");

        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
//        when(movieRepository.save(any(Movie.class))).thenReturn(testMovie);
//        when(movieRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        when(movieRepository.save(movieCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        Movie result = movieService.updateMovie(1L, request);

        assertNotNull(result);
        assertEquals("Inception Updated", result.getTitle());
        assertEquals("Chris Nolan", result.getDirector());
        verify(movieRepository, times(1)).findById(1L);
        verify(movieRepository, times(1)).save(testMovie);

    }

    @Test
    void updateMovie_PartialUpdate_OnlyUpdatesProvidedFields() {
        MovieRequest request = new MovieRequest();
        request.setTitle("New Title");
        request.setDirector(null);  // Don't update director

        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(movieRepository.save(any(Movie.class))).thenReturn(testMovie);

        Movie result = movieService.updateMovie(1L, request);

        // Verify the movie's setTitle was called but not setDirector
        assertEquals("New Title", result.getTitle());  // Title should be updated
        assertEquals("Christopher Nolan", result.getDirector());  // Director should NOT change
        verify(movieRepository, times(1)).findById(1L);
        verify(movieRepository, times(1)).save(testMovie);
    }

    @Test
    void updateMovie_MovieNotFound_ThrowsException() {
        MovieRequest request = new MovieRequest();
        request.setTitle("Updated Title");

        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            movieService.updateMovie(999L, request);
        });

        assertTrue(exception.getMessage().contains("not found"));
        verify(movieRepository, times(1)).findById(999L);
        verify(movieRepository, never()).save(any(Movie.class));
    }

    @Test
    void deleteMovie_Success() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(showtimeRepository.existsFutureShowtimeForMovie(eq(1L), any(LocalDateTime.class))).thenReturn(false);
        when(movieRepository.save(testMovie)).thenReturn(testMovie);

        movieService.deleteMovie(1L);

        verify(movieRepository, times(1)).findById(1L);
        verify(movieRepository, times(1)).save(testMovie);
        assertFalse(testMovie.isActive());
    }

    @Test
    void deleteMovie_WithFutureShowtime_IsRejected() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(showtimeRepository.existsFutureShowtimeForMovie(eq(1L), any(LocalDateTime.class))).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> movieService.deleteMovie(1L)
        );

        assertTrue(exception.getMessage().contains("future showtimes"));
        verify(movieRepository, never()).save(any(Movie.class));
    }

    @Test
    void deleteMovie_MovieNotFound_ThrowsException() {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            movieService.deleteMovie(999L);
        });

        assertTrue(exception.getMessage().contains("not found"));
        verify(movieRepository, times(1)).findById(999L);
        verify(movieRepository, never()).delete(any(Movie.class));
    }
}
