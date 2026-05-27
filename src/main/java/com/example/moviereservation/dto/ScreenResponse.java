package com.example.moviereservation.dto;

import com.example.moviereservation.entity.ScreenType;

public class ScreenResponse {
    private Long id;
    private String name;
    private TheatreSummary theatre;
    private Integer totalSeats;
    private ScreenType screenType;
    private boolean active;

    public ScreenResponse() {
    }

    public ScreenResponse(Long id, String name, TheatreSummary theatre, Integer totalSeats, ScreenType screenType, boolean active) {
        this.id = id;
        this.name = name;
        this.theatre = theatre;
        this.totalSeats = totalSeats;
        this.screenType = screenType;
        this.active = active;
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

    public TheatreSummary getTheatre() {
        return theatre;
    }

    public void setTheatre(TheatreSummary theatre) {
        this.theatre = theatre;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public ScreenType getScreenType() {
        return screenType;
    }

    public void setScreenType(ScreenType screenType) {
        this.screenType = screenType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
}
