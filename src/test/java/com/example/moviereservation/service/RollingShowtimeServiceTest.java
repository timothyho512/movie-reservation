package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RollingShowtimeServiceTest {
    @Mock private MovieRepository movieRepository;
    @Mock private ScreenRepository screenRepository;
    @Mock private ShowtimeRepository showtimeRepository;

    @Test
    void generatesTwoDailySlotsForFourteenDays() {
        Movie movie = new Movie("Current Film", "Director");
        movie.setRuntimeMinutes(150);
        Screen screen = screen(ScreenType.IMAX);
        when(movieRepository.findAllByActiveTrueAndNowPlayingTrueOrderByTitleAsc()).thenReturn(List.of(movie));
        when(screenRepository.findActiveSchedulingScreens()).thenReturn(List.of(screen));
        when(showtimeRepository.existsOverlappingShowtime(any(), any(), any())).thenReturn(false);

        RollingShowtimeService.GenerationResult result = service().ensureFutureShowtimes(
                LocalDateTime.of(2026, 7, 20, 10, 0)
        );

        assertThat(result.generatedCount()).isEqualTo(28);
        ArgumentCaptor<List<Showtime>> captor = ArgumentCaptor.forClass(List.class);
        verify(showtimeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(28);
        Showtime first = captor.getValue().getFirst();
        assertThat(first.getStartTime()).isEqualTo(LocalDateTime.of(2026, 7, 21, 14, 0));
        assertThat(first.getEndTime()).isEqualTo(LocalDateTime.of(2026, 7, 21, 16, 50));
        assertThat(first.getBasePrice()).isEqualByComparingTo("19.00");
    }

    @Test
    void existingSlotsAreNotDuplicated() {
        Movie movie = new Movie("Current Film", "Director");
        Screen screen = screen(ScreenType.STANDARD);
        when(movieRepository.findAllByActiveTrueAndNowPlayingTrueOrderByTitleAsc()).thenReturn(List.of(movie));
        when(screenRepository.findActiveSchedulingScreens()).thenReturn(List.of(screen));
        when(showtimeRepository.existsOverlappingShowtime(any(), any(), any())).thenReturn(true);

        RollingShowtimeService.GenerationResult result = service().ensureFutureShowtimes(LocalDateTime.now());

        assertThat(result.generatedCount()).isZero();
        verify(showtimeRepository).saveAll(List.of());
    }

    @Test
    void noCatalogueSkipsGeneration() {
        when(movieRepository.findAllByActiveTrueAndNowPlayingTrueOrderByTitleAsc()).thenReturn(List.of());
        when(movieRepository.findAllByActiveTrueOrderByTitleAsc()).thenReturn(List.of());
        when(screenRepository.findActiveSchedulingScreens()).thenReturn(List.of(screen(ScreenType.STANDARD)));

        RollingShowtimeService.GenerationResult result = service().ensureFutureShowtimes(LocalDateTime.now());

        assertThat(result.generatedCount()).isZero();
        verify(showtimeRepository, never()).saveAll(anyList());
    }

    private RollingShowtimeService service() {
        return new RollingShowtimeService(movieRepository, screenRepository, showtimeRepository);
    }

    private Screen screen(ScreenType type) {
        Theatre theatre = new Theatre("Cinema", "Address", "London", "England", "UK", "SW1A 1AA");
        Screen screen = new Screen("Screen 1", theatre, 30, type);
        screen.setId(1L);
        return screen;
    }
}
