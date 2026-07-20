package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.ProgrammeEntry;
import com.example.moviereservation.entity.Screen;
import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.Showtime;
import com.example.moviereservation.entity.Theatre;
import com.example.moviereservation.repository.ScreenRepository;
import com.example.moviereservation.repository.ShowtimeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyShowtimeGeneratorTest {
    @Mock private ScreenRepository screenRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private BookingWindowService bookingWindowService;

    @Test
    void generatesTwoDailySlotsForOneProgrammeWeek() {
        LocalDate startsOn = LocalDate.of(2026, 7, 24);
        Movie movie = movie(1L, "Current Film", 150);
        ProgrammeEntry entry = entry(1L, movie, startsOn);
        Screen screen = screen(ScreenType.IMAX);
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);

        when(screenRepository.findActiveSchedulingScreens()).thenReturn(List.of(screen));
        when(bookingWindowService.bookingCutoffFrom(now)).thenReturn(now.plusMinutes(10));
        when(showtimeRepository.existsOverlappingShowtime(any(), any(), any())).thenReturn(false);

        WeeklyShowtimeGenerator.GenerationResult result = service().ensureShowtimes(
                List.of(entry), startsOn, startsOn.plusDays(6), now
        );

        assertThat(result.generatedCount()).isEqualTo(14);
        ArgumentCaptor<List<Showtime>> captor = ArgumentCaptor.forClass(List.class);
        verify(showtimeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(14);
        Showtime first = captor.getValue().getFirst();
        assertThat(first.getStartTime()).isEqualTo(LocalDateTime.of(2026, 7, 24, 14, 0));
        assertThat(first.getEndTime()).isEqualTo(LocalDateTime.of(2026, 7, 24, 16, 50));
        assertThat(first.getBasePrice()).isEqualByComparingTo("19.00");
        assertThat(first.getMovie()).isSameAs(movie);
        assertThat(first.getProgrammeEntry()).isSameAs(entry);
    }

    @Test
    void existingSlotsAreNotDuplicated() {
        LocalDate startsOn = LocalDate.of(2026, 7, 24);
        Movie movie = movie(1L, "Current Film", 120);
        ProgrammeEntry entry = entry(1L, movie, startsOn);
        Screen screen = screen(ScreenType.STANDARD);
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);

        when(screenRepository.findActiveSchedulingScreens()).thenReturn(List.of(screen));
        when(bookingWindowService.bookingCutoffFrom(now)).thenReturn(now.plusMinutes(10));
        when(showtimeRepository.existsOverlappingShowtime(any(), any(), any())).thenReturn(true);

        WeeklyShowtimeGenerator.GenerationResult result = service().ensureShowtimes(
                List.of(entry), startsOn, startsOn.plusDays(6), now
        );

        assertThat(result.generatedCount()).isZero();
        verify(showtimeRepository, never()).saveAll(any());
    }

    @Test
    void noProgrammeSkipsGeneration() {
        WeeklyShowtimeGenerator.GenerationResult result = service().ensureShowtimes(
                List.of(),
                LocalDate.of(2026, 7, 24),
                LocalDate.of(2026, 7, 30),
                LocalDateTime.of(2026, 7, 20, 10, 0)
        );

        assertThat(result.generatedCount()).isZero();
        verify(showtimeRepository, never()).saveAll(any());
    }

    private WeeklyShowtimeGenerator service() {
        return new WeeklyShowtimeGenerator(screenRepository, showtimeRepository, bookingWindowService);
    }

    private Movie movie(Long id, String title, int runtimeMinutes) {
        Movie movie = new Movie(title, "Director");
        movie.setId(id);
        movie.setRuntimeMinutes(runtimeMinutes);
        return movie;
    }

    private ProgrammeEntry entry(Long id, Movie movie, LocalDate startsOn) {
        ProgrammeEntry entry = new ProgrammeEntry(movie, startsOn, startsOn.plusDays(6));
        entry.setId(id);
        return entry;
    }

    private Screen screen(ScreenType type) {
        Theatre theatre = new Theatre("Cinema", "Address", "London", "England", "UK", "SW1A 1AA");
        Screen screen = new Screen("Screen 1", theatre, 30, type);
        screen.setId(1L);
        return screen;
    }
}
