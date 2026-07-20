package com.example.moviereservation.service;

import com.example.moviereservation.config.TmdbProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class TmdbHttpClient implements TmdbGateway {
    private final TmdbProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TmdbHttpClient(TmdbProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .build();
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public List<TmdbMovie> fetchNowPlaying() {
        if (!isConfigured()) {
            return List.of();
        }

        JsonNode response = getJson("/movie/now_playing?region=" + encode(properties.getRegion())
                + "&language=" + encode(properties.getLanguage()) + "&page=1");
        JsonNode results = response.path("results");
        if (!results.isArray()) {
            throw new IllegalStateException("TMDB now-playing response did not contain a results array");
        }

        List<TmdbMovie> movies = new ArrayList<>();
        for (JsonNode summary : results) {
            String posterPath = textOrNull(summary, "poster_path");
            if (posterPath == null) {
                continue;
            }
            movies.add(fetchMovieDetails(summary.path("id").asLong()));
            if (movies.size() >= Math.max(1, properties.getCatalogueSize())) {
                break;
            }
        }
        return List.copyOf(movies);
    }

    private TmdbMovie fetchMovieDetails(long tmdbId) {
        JsonNode movie = getJson("/movie/" + tmdbId + "?language=" + encode(properties.getLanguage())
                + "&append_to_response=credits");
        return new TmdbMovie(
                tmdbId,
                movie.path("title").asText("Untitled"),
                director(movie.path("credits").path("crew")),
                textOrNull(movie, "poster_path"),
                movie.path("overview").asText(""),
                parseDate(textOrNull(movie, "release_date")),
                Math.max(movie.path("runtime").asInt(120), 1)
        );
    }

    private String director(JsonNode crew) {
        if (crew.isArray()) {
            for (JsonNode member : crew) {
                if ("Director".equals(member.path("job").asText())) {
                    return member.path("name").asText("Unknown director");
                }
            }
        }
        return "Unknown director";
    }

    private JsonNode getJson(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getApiBaseUrl() + path))
                .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("TMDB request failed with HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TMDB request was interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("TMDB request failed", exception);
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
