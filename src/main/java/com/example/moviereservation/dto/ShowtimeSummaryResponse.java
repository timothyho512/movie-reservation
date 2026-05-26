package com.example.moviereservation.dto;

import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShowtimeSummaryResponse {
    private Long id;
    private MovieSummary movie;
    private TheatreSummary theatre;
    private ScreenSummary screen;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private Integer availableSeats;
    private Integer totalSeats;
    private ShowtimeStatus status;

    public ShowtimeSummaryResponse() {
    }

    public ShowtimeSummaryResponse(
            Long id,
            MovieSummary movie,
            TheatreSummary theatre,
            ScreenSummary screen,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal basePrice,
            Integer availableSeats,
            Integer totalSeats,
            ShowtimeStatus status
    ) {
        this.id = id;
        this.movie = movie;
        this.theatre = theatre;
        this.screen = screen;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
        this.availableSeats = availableSeats;
        this.totalSeats = totalSeats;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MovieSummary getMovie() {
        return movie;
    }

    public void setMovie(MovieSummary movie) {
        this.movie = movie;
    }

    public TheatreSummary getTheatre() {
        return theatre;
    }

    public void setTheatre(TheatreSummary theatre) {
        this.theatre = theatre;
    }

    public ScreenSummary getScreen() {
        return screen;
    }

    public void setScreen(ScreenSummary screen) {
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

    public static class MovieSummary {
        private Long id;
        private String title;
        private String director;

        public MovieSummary() {
        }

        public MovieSummary(Long id, String title, String director) {
            this.id = id;
            this.title = title;
            this.director = director;
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
    }

    public static class TheatreSummary {
        private Long id;
        private String name;
        private String city;
        private String country;

        public TheatreSummary() {
        }

        public TheatreSummary(Long id, String name, String city, String country) {
            this.id = id;
            this.name = name;
            this.city = city;
            this.country = country;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }

    public static class ScreenSummary {
        private Long id;
        private String name;
        private ScreenType screenType;

        public ScreenSummary() {
        }

        public ScreenSummary(Long id, String name, ScreenType screenType) {
            this.id = id;
            this.name = name;
            this.screenType = screenType;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ScreenType getScreenType() {
            return screenType;
        }

        public void setScreenType(ScreenType screenType) {
            this.screenType = screenType;
        }
    }
}
