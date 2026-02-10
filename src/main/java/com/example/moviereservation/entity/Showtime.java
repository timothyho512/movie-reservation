package com.example.moviereservation.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    @JsonBackReference
    private Movie movie;  // Which movie is showing

    @ManyToOne
    @JoinColumn(name = "screen_id", nullable = false)
    @JsonBackReference
    private Screen screen;  // Which screen it's showing in

    @Column(nullable = false)
    private LocalDateTime startTime;  // When movie starts

    @Column(nullable = false)
    private LocalDateTime endTime;    // When movie ends

    @Column(nullable = false)
    private BigDecimal basePrice;  // Base ticket price (can vary by showtime)

    @Column(nullable = false)
    private Integer availableSeats;  // Number of seats still available

    @Column(nullable = false)
    private Integer totalSeats;  // Total seats for this showtime

    @Enumerated(EnumType.STRING)
    private ShowtimeStatus status;  // UPCOMING, ONGOING, COMPLETED, CANCELLED

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Constructors
    public Showtime() {
    }

    public Showtime(Movie movie, Screen screen, LocalDateTime startTime, LocalDateTime endTime, BigDecimal basePrice) {
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
        this.totalSeats = screen.getTotalSeats();
        this.availableSeats = this.totalSeats;
        this.status = ShowtimeStatus.UPCOMING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public ShowtimeStatus getStatus() {
        return status;
    }

    public void setStatus(ShowtimeStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
