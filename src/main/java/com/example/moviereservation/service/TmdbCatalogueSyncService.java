package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TmdbCatalogueSyncService {
    private static final Logger logger = LoggerFactory.getLogger(TmdbCatalogueSyncService.class);

    private final TmdbGateway tmdbGateway;
    private final MovieRepository movieRepository;

    public TmdbCatalogueSyncService(TmdbGateway tmdbGateway, MovieRepository movieRepository) {
        this.tmdbGateway = tmdbGateway;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public SyncResult synchronize() {
        if (!tmdbGateway.isConfigured()) {
            logger.info("event=tmdb_catalogue_sync_skipped reason=token_not_configured");
            return new SyncResult(false, 0);
        }

        final List<TmdbGateway.TmdbMovie> currentMovies;
        try {
            currentMovies = tmdbGateway.fetchNowPlaying();
        } catch (RuntimeException exception) {
            logger.warn("event=tmdb_catalogue_sync_failed reason={}", exception.getMessage());
            return new SyncResult(false, 0);
        }
        if (currentMovies.isEmpty()) {
            logger.warn("event=tmdb_catalogue_sync_failed reason=empty_catalogue");
            return new SyncResult(false, 0);
        }

        LocalDateTime syncedAt = LocalDateTime.now();
        List<Movie> managedMovies = new ArrayList<>(movieRepository.findAllByTmdbManagedTrue());
        Map<Long, Movie> byTmdbId = new HashMap<>();
        for (Movie movie : managedMovies) {
            movie.setNowPlaying(false);
            byTmdbId.put(movie.getTmdbId(), movie);
        }

        for (TmdbGateway.TmdbMovie incoming : currentMovies) {
            Movie movie = byTmdbId.get(incoming.tmdbId());
            if (movie == null) {
                movie = new Movie();
                movie.setTmdbId(incoming.tmdbId());
                movie.setTmdbManaged(true);
                managedMovies.add(movie);
            }
            movie.setTitle(incoming.title());
            movie.setDirector(incoming.director());
            movie.setPosterPath(incoming.posterPath());
            movie.setOverview(incoming.overview());
            movie.setReleaseDate(incoming.releaseDate());
            movie.setRuntimeMinutes(incoming.runtimeMinutes());
            movie.setNowPlaying(true);
            movie.setActive(true);
            movie.setLastSyncedAt(syncedAt);
        }

        movieRepository.saveAll(managedMovies);
        logger.info("event=tmdb_catalogue_synchronized movieCount={}", currentMovies.size());
        return new SyncResult(true, currentMovies.size());
    }

    public record SyncResult(boolean synchronizedSuccessfully, int movieCount) {
    }
}
