package com.example.moviereservation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String director;

    // Constructor
    public Movie() {
        // JPA requires a no-arg constructor
    }

    // if later, want to create construct instance faster
    public Movie(String title, String director) {
        this.title = title;
        this.director = director;
    }

    // Getters and Setters
    // usually, the id is fixed and created by postgres
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
}
