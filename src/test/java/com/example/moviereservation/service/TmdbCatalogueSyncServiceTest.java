package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmdbCatalogueSyncServiceTest {
    @Mock private TmdbGateway tmdbGateway;
    @Mock private MovieRepository movieRepository;

    @Test
    void missingTokenPreservesExistingCatalogue() {
        when(tmdbGateway.isConfigured()).thenReturn(false);

        TmdbCatalogueSyncService.SyncResult result = service().synchronize();

        assertThat(result.synchronizedSuccessfully()).isFalse();
        verify(movieRepository, never()).saveAll(anyList());
    }

    @Test
    void successfulSyncUpsertsMoviesAndClearsPreviousNowPlayingFlag() {
        Movie previous = new Movie("Previous Film", "Director");
        previous.setTmdbId(10L);
        previous.setTmdbManaged(true);
        previous.setNowPlaying(true);

        when(tmdbGateway.isConfigured()).thenReturn(true);
        when(tmdbGateway.fetchNowPlaying()).thenReturn(List.of(
                new TmdbGateway.TmdbMovie(
                        20L,
                        "Current Film",
                        "Current Director",
                        "/poster.jpg",
                        "Overview",
                        LocalDate.of(2026, 7, 1),
                        142
                )
        ));
        when(movieRepository.findAllByTmdbManagedTrue()).thenReturn(List.of(previous));

        TmdbCatalogueSyncService.SyncResult result = service().synchronize();

        assertThat(result.synchronizedSuccessfully()).isTrue();
        assertThat(result.movieCount()).isEqualTo(1);
        ArgumentCaptor<List<Movie>> captor = ArgumentCaptor.forClass(List.class);
        verify(movieRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(previous.isNowPlaying()).isFalse();
        Movie current = captor.getValue().stream()
                .filter(movie -> Long.valueOf(20L).equals(movie.getTmdbId()))
                .findFirst()
                .orElseThrow();
        assertThat(current.getTitle()).isEqualTo("Current Film");
        assertThat(current.getPosterPath()).isEqualTo("/poster.jpg");
        assertThat(current.getRuntimeMinutes()).isEqualTo(142);
        assertThat(current.isNowPlaying()).isTrue();
        assertThat(current.isTmdbManaged()).isTrue();
        assertThat(current.isActive()).isTrue();
    }

    @Test
    void failedRequestPreservesLastSuccessfulCatalogue() {
        when(tmdbGateway.isConfigured()).thenReturn(true);
        when(tmdbGateway.fetchNowPlaying()).thenThrow(new IllegalStateException("unavailable"));

        TmdbCatalogueSyncService.SyncResult result = service().synchronize();

        assertThat(result.synchronizedSuccessfully()).isFalse();
        verify(movieRepository, never()).findAllByTmdbManagedTrue();
        verify(movieRepository, never()).saveAll(anyList());
    }

    @Test
    void synchronizationDoesNotReactivateAdministrativelyDisabledMovie() {
        Movie disabled = new Movie("Disabled Film", "Director");
        disabled.setTmdbId(20L);
        disabled.setTmdbManaged(true);
        disabled.setActive(false);

        when(tmdbGateway.isConfigured()).thenReturn(true);
        when(tmdbGateway.fetchNowPlaying()).thenReturn(List.of(
                new TmdbGateway.TmdbMovie(
                        20L,
                        "Disabled Film",
                        "Director",
                        "/poster.jpg",
                        "Overview",
                        LocalDate.of(2026, 7, 1),
                        120
                )
        ));
        when(movieRepository.findAllByTmdbManagedTrue()).thenReturn(List.of(disabled));

        service().synchronize();

        assertThat(disabled.isNowPlaying()).isTrue();
        assertThat(disabled.isActive()).isFalse();
    }

    @Test
    void startupSkipsExternalSynchronizationWhenCatalogueIsFresh() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 20, 10, 0);
        Movie recentlySynced = new Movie("Current Film", "Director");
        recentlySynced.setTmdbManaged(true);
        recentlySynced.setLastSyncedAt(now.minusHours(2));

        when(tmdbGateway.isConfigured()).thenReturn(true);
        when(movieRepository.findFirstByTmdbManagedTrueAndLastSyncedAtIsNotNullOrderByLastSyncedAtDesc())
                .thenReturn(Optional.of(recentlySynced));

        TmdbCatalogueSyncService.SyncResult result = service().synchronizeIfStale(now);

        assertThat(result.synchronizedSuccessfully()).isFalse();
        verify(tmdbGateway, never()).fetchNowPlaying();
    }

    private TmdbCatalogueSyncService service() {
        return new TmdbCatalogueSyncService(tmdbGateway, movieRepository);
    }
}
