package com.example.moviereservation.dto;

import java.util.List;

public class AdminSeatLayoutResponse {
    private Long screenId;
    private Long layoutVersionId;
    private Integer versionNumber;
    private Integer totalSeats;
    private List<SeatResponse> seats;

    public AdminSeatLayoutResponse(Long screenId, Long layoutVersionId, Integer versionNumber, Integer totalSeats, List<SeatResponse> seats) {
        this.screenId = screenId;
        this.layoutVersionId = layoutVersionId;
        this.versionNumber = versionNumber;
        this.totalSeats = totalSeats;
        this.seats = seats;
    }

    public Long getScreenId() {
        return screenId;
    }

    public Long getLayoutVersionId() {
        return layoutVersionId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public List<SeatResponse> getSeats() {
        return seats;
    }
}
