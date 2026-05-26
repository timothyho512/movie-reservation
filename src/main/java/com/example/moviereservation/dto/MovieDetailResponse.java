package com.example.moviereservation.dto;

import java.util.List;

public class MovieDetailResponse {
    private Long id;
    private String title;
    private String director;
    private List<ShowtimeSummaryResponse> showtimes;

    public MovieDetailResponse() {
    }

    public MovieDetailResponse(Long id, String title, String director, List<ShowtimeSummaryResponse> showtimes) {
        this.id = id;
        this.title = title;
        this.director = director;
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

    public List<ShowtimeSummaryResponse> getShowtimes() {
        return showtimes;
    }

    public void setShowtimes(List<ShowtimeSummaryResponse> showtimes) {
        this.showtimes = showtimes;
    }
}
