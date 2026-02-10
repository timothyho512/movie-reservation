package com.example.moviereservation.dto;

import com.example.moviereservation.entity.SeatType;

import java.math.BigDecimal;

public class SeatRequest {
    private Long screenId;
    private String rowLabel;
    private Integer seatNumber;
    private SeatType seatType;
    private BigDecimal basePrice;

    // Constructors
    public SeatRequest() {
    }

    public SeatRequest(Long screenId, String rowLabel, Integer seatNumber, SeatType seatType, BigDecimal basePrice) {
        this.screenId = screenId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.basePrice = basePrice;
    }

    public Long getScreenId() {
        return screenId;
    }

    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void setSeatType(SeatType seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
}
