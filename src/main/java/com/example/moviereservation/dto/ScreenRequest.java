package com.example.moviereservation.dto;

import com.example.moviereservation.entity.ScreenType;

public class ScreenRequest {
    private String name;
    private Long theatreId;  // ← Just the ID, not the whole Theatre object
    private Integer totalSeats;
    private ScreenType screenType;

    // Constructors
    public ScreenRequest() {
    }

    public ScreenRequest(String name, Long theatreId, Integer totalSeats, ScreenType screenType) {
        this.name = name;
        this.theatreId = theatreId;
        this.totalSeats = totalSeats;
        this.screenType = screenType;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTheatreId() {
        return theatreId;
    }

    public void setTheatreId(Long theatreId) {
        this.theatreId = theatreId;
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
}
