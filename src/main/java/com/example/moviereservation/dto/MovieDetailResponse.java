package com.example.moviereservation.dto;

import java.util.List;

public class MovieDetailResponse {
    private Long id;
    private String title;
    private String director;
    private String posterPath;
    private String overview;
    private Integer runtimeMinutes;
    private List<ShowtimeSummaryResponse> showtimes;

    public MovieDetailResponse() {
    }

    public MovieDetailResponse(
            Long id,
            String title,
            String director,
            String posterPath,
            String overview,
            Integer runtimeMinutes,
            List<ShowtimeSummaryResponse> showtimes
    ) {
        this.id = id;
        this.title = title;
        this.director = director;
        this.posterPath = posterPath;
        this.overview = overview;
        this.runtimeMinutes = runtimeMinutes;
        this.showtimes = showtimes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public Integer getRuntimeMinutes() {
        return runtimeMinutes;
    }

    public void setRuntimeMinutes(Integer runtimeMinutes) {
        this.runtimeMinutes = runtimeMinutes;
    }

    public List<ShowtimeSummaryResponse> getShowtimes() {
        return showtimes;
    }

    public void setShowtimes(List<ShowtimeSummaryResponse> showtimes) {
        this.showtimes = showtimes;
    }
}
