package com.example.moviereservation.dto;

import com.example.moviereservation.entity.ScreenType;

import java.util.List;

public class TheatreDetailResponse extends TheatreSummaryResponse {
    private List<ScreenSummary> screens;

    public TheatreDetailResponse() {
    }

    public TheatreDetailResponse(
            Long id,
            String name,
            String address,
            String city,
            String state,
            String country,
            String postalCode,
            String phoneNumber,
            Integer totalScreens,
            Integer totalSeats,
            boolean active,
            List<ScreenSummary> screens
    ) {
        super(id, name, address, city, state, country, postalCode, phoneNumber, totalScreens, totalSeats, active);
        this.screens = screens;
    }

    public List<ScreenSummary> getScreens() {
        return screens;
    }

    public void setScreens(List<ScreenSummary> screens) {
        this.screens = screens;
    }

    public static class ScreenSummary {
        private Long id;
        private String name;
        private Integer totalSeats;
        private ScreenType screenType;
        private boolean active;

        public ScreenSummary() {
        }

        public ScreenSummary(Long id, String name, Integer totalSeats, ScreenType screenType, boolean active) {
            this.id = id;
            this.name = name;
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
    }
}
