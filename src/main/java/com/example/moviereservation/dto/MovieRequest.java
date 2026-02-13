package com.example.moviereservation.dto;

public class MovieRequest {
    private String title;
    private String director;

    // Constructors
    public MovieRequest() {
    }

    public MovieRequest(String title, String director) {
        this.title = title;
        this.director = director;
    }

    // Getters and Setters
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
}
