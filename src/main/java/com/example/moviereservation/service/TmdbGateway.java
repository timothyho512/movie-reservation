package com.example.moviereservation.service;

import java.time.LocalDate;
import java.util.List;

public interface TmdbGateway {
    boolean isConfigured();

    List<TmdbMovie> fetchNowPlaying();

    record TmdbMovie(
            long tmdbId,
            String title,
            String director,
            String posterPath,
            String overview,
            LocalDate releaseDate,
            int runtimeMinutes
    ) {
    }
}
