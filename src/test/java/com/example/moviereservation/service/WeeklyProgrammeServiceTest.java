package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.ProgrammeEntry;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ProgrammeEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyProgrammeServiceTest {
    @Mock private MovieRepository movieRepository;
    @Mock private ProgrammeEntryRepository programmeEntryRepository;

    @Test
    void firstProgrammeSelectsFourMostRecentCandidates() {
        LocalDate startsOn = LocalDate.of(2026, 7, 24);
        Movie oldest = movie(1L, "Oldest", LocalDate.of(2026, 6, 1), true);
        Movie second = movie(2L, "Second", LocalDate.of(2026, 7, 1), true);
        Movie newest = movie(3L, "Newest", LocalDate.of(2026, 7, 20), true);
        Movie third = movie(4L, "Third", LocalDate.of(2026, 7, 10), true);
        Movie future = movie(5L, "Future", LocalDate.of(2026, 8, 1), true);

        when(programmeEntryRepository.findProgrammeStartingOn(startsOn))
                .thenReturn(List.of())
                .thenAnswer(invocation -> savedEntries(startsOn));
        when(programmeEntryRepository.findTopByStartsOnBeforeOrderByStartsOnDesc(startsOn))
                .thenReturn(Optional.empty());
        when(movieRepository.findAllByActiveTrueAndNowPlayingTrueOrderByTitleAsc())
                .thenReturn(List.of(oldest, second, newest, third, future));
        persistSavedEntries();

        List<ProgrammeEntry> result = service().ensureProgramme(startsOn);

        assertThat(result).hasSize(4);
        ArgumentCaptor<List<ProgrammeEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(programmeEntryRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(entry -> entry.getMovie().getTitle())
                .containsExactly("Newest", "Third", "Second", "Oldest");
        assertThat(captor.getValue()).allSatisfy(entry -> {
            assertThat(entry.getStartsOn()).isEqualTo(startsOn);
            assertThat(entry.getEndsOn()).isEqualTo(startsOn.plusDays(6));
        });
    }

    @Test
    void existingCompleteProgrammeIsIdempotent() {
        LocalDate startsOn = LocalDate.of(2026, 7, 24);
        List<ProgrammeEntry> existing = entries(startsOn,
                movie(1L, "A", startsOn.minusDays(10), true),
                movie(2L, "B", startsOn.minusDays(9), true),
                movie(3L, "C", startsOn.minusDays(8), true),
                movie(4L, "D", startsOn.minusDays(7), true)
        );
        when(programmeEntryRepository.findProgrammeStartingOn(startsOn)).thenReturn(existing);

        List<ProgrammeEntry> result = service().ensureProgramme(startsOn);

        assertThat(result).isSameAs(existing);
        verify(programmeEntryRepository, never()).saveAll(anyList());
    }

    @Test
    void movieCannotBeReplacedBeforeCompletingTwoWeeks() {
        LocalDate startsOn = LocalDate.of(2026, 8, 7);
        LocalDate previousStartsOn = startsOn.minusWeeks(1);
        List<Movie> previousMovies = List.of(
                movie(1L, "A", LocalDate.of(2026, 7, 1), true),
                movie(2L, "B", LocalDate.of(2026, 7, 2), true),
                movie(3L, "C", LocalDate.of(2026, 7, 3), true),
                movie(4L, "D", LocalDate.of(2026, 7, 4), true)
        );
        Movie replacement = movie(5L, "New Release", LocalDate.of(2026, 8, 5), true);
        ProgrammeEntry previousMarker = new ProgrammeEntry(previousMovies.getFirst(), previousStartsOn, previousStartsOn.plusDays(6));

        when(programmeEntryRepository.findProgrammeStartingOn(startsOn))
                .thenReturn(List.of())
                .thenAnswer(invocation -> savedEntries(startsOn));
        when(programmeEntryRepository.findTopByStartsOnBeforeOrderByStartsOnDesc(startsOn))
                .thenReturn(Optional.of(previousMarker));
        when(programmeEntryRepository.findProgrammeStartingOn(previousStartsOn))
                .thenReturn(entries(previousStartsOn, previousMovies.toArray(Movie[]::new)));
        when(programmeEntryRepository.findProgrammeStartingOn(startsOn.minusWeeks(2))).thenReturn(List.of());
        when(movieRepository.findAllByActiveTrueAndNowPlayingTrueOrderByTitleAsc())
                .thenReturn(concat(previousMovies, replacement));
        when(programmeEntryRepository.findAllProgrammedMovieIds())
                .thenReturn(previousMovies.stream().map(Movie::getId).toList());
        persistSavedEntries();

        service().ensureProgramme(startsOn);

        ArgumentCaptor<List<ProgrammeEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(programmeEntryRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(entry -> entry.getMovie().getTitle())
                .containsExactlyInAnyOrder("A", "B", "C", "D");
    }

    @Test
    void replacesAtMostOneEligibleDepartedMovie() {
        LocalDate startsOn = LocalDate.of(2026, 8, 7);
        LocalDate previousStartsOn = startsOn.minusWeeks(1);
        LocalDate twoWeeksAgoStartsOn = startsOn.minusWeeks(2);
        Movie departed = movie(1L, "Departed", LocalDate.of(2026, 6, 1), false);
        Movie b = movie(2L, "B", LocalDate.of(2026, 7, 2), true);
        Movie c = movie(3L, "C", LocalDate.of(2026, 7, 3), true);
        Movie d = movie(4L, "D", LocalDate.of(2026, 7, 4), true);
        Movie replacement = movie(5L, "New Release", LocalDate.of(2026, 8, 5), true);
        List<Movie> previousMovies = List.of(departed, b, c, d);
        ProgrammeEntry previousMarker = new ProgrammeEntry(departed, previousStartsOn, previousStartsOn.plusDays(6));

        when(programmeEntryRepository.findProgrammeStartingOn(startsOn))
                .thenReturn(List.of())
                .thenAnswer(invocation -> savedEntries(startsOn));
        when(programmeEntryRepository.findTopByStartsOnBeforeOrderByStartsOnDesc(startsOn))
                .thenReturn(Optional.of(previousMarker));
        when(programmeEntryRepository.findProgrammeStartingOn(previousStartsOn))
                .thenReturn(entries(previousStartsOn, previousMovies.toArray(Movie[]::new)));
        when(programmeEntryRepository.findProgrammeStartingOn(twoWeeksAgoStartsOn))
                .thenReturn(entries(twoWeeksAgoStartsOn, previousMovies.toArray(Movie[]::new)));
        when(movieRepository.findAllByActiveTrueAndNowPlayingTrueOrderByTitleAsc())
                .thenReturn(List.of(b, c, d, replacement));
        when(programmeEntryRepository.findAllProgrammedMovieIds())
                .thenReturn(previousMovies.stream().map(Movie::getId).toList());
        when(programmeEntryRepository.findAllByMovieIdOrderByStartsOnDesc(anyLong()))
                .thenAnswer(invocation -> {
                    Long movieId = invocation.getArgument(0);
                    Movie movie = previousMovies.stream()
                            .filter(candidate -> candidate.getId().equals(movieId))
                            .findFirst()
                            .orElseThrow();
                    return List.of(
                            new ProgrammeEntry(movie, previousStartsOn, previousStartsOn.plusDays(6)),
                            new ProgrammeEntry(movie, twoWeeksAgoStartsOn, twoWeeksAgoStartsOn.plusDays(6))
                    );
                });
        persistSavedEntries();

        service().ensureProgramme(startsOn);

        ArgumentCaptor<List<ProgrammeEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(programmeEntryRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(entry -> entry.getMovie().getTitle())
                .containsExactlyInAnyOrder("B", "C", "D", "New Release");
    }

    private List<ProgrammeEntry> lastSaved = List.of();

    private WeeklyProgrammeService service() {
        return new WeeklyProgrammeService(movieRepository, programmeEntryRepository);
    }

    private void persistSavedEntries() {
        when(programmeEntryRepository.saveAll(anyList())).thenAnswer(invocation -> {
            lastSaved = new ArrayList<>(invocation.getArgument(0));
            return lastSaved;
        });
    }

    private List<ProgrammeEntry> savedEntries(LocalDate startsOn) {
        return lastSaved.stream().filter(entry -> entry.getStartsOn().equals(startsOn)).toList();
    }

    private List<ProgrammeEntry> entries(LocalDate startsOn, Movie... movies) {
        return List.of(movies).stream()
                .map(movie -> new ProgrammeEntry(movie, startsOn, startsOn.plusDays(6)))
                .toList();
    }

    private Movie movie(Long id, String title, LocalDate releaseDate, boolean nowPlaying) {
        Movie movie = new Movie(title, "Director");
        movie.setId(id);
        movie.setReleaseDate(releaseDate);
        movie.setNowPlaying(nowPlaying);
        return movie;
    }

    private List<Movie> concat(List<Movie> movies, Movie extra) {
        List<Movie> result = new ArrayList<>(movies);
        result.add(extra);
        return result;
    }
}
