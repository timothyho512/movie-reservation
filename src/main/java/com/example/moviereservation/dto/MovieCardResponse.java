package com.example.moviereservation.dto;

public class MovieCardResponse {
    private Long id;
    private String title;
    private String director;
    private String posterPath;

    public MovieCardResponse() {
    }

    public MovieCardResponse(Long id, String title, String director, String posterPath) {
        this.id = id;
        this.title = title;
        this.director = director;
        this.posterPath = posterPath;
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
}
