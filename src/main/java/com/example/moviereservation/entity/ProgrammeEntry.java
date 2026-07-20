package com.example.moviereservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "programme_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_programme_entry_movie_week",
                columnNames = {"movie_id", "starts_on"}
        )
)
public class ProgrammeEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private LocalDate startsOn;

    @Column(nullable = false)
    private LocalDate endsOn;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ProgrammeEntry() {
        // JPA requires a no-arg constructor.
    }

    public ProgrammeEntry(Movie movie, LocalDate startsOn, LocalDate endsOn) {
        this.movie = movie;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public void setStartsOn(LocalDate startsOn) {
        this.startsOn = startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public void setEndsOn(LocalDate endsOn) {
        this.endsOn = endsOn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
