package com.example.moviereservation.dto;

import com.example.moviereservation.entity.ScreenType;
import com.example.moviereservation.entity.SeatType;

import java.math.BigDecimal;

public class SeatResponse {
    private Long id;
    private ScreenSummary screen;
    private String rowLabel;
    private Integer seatNumber;
    private SeatType seatType;
    private BigDecimal basePrice;
    private boolean active;

    public SeatResponse() {
    }

    public SeatResponse(
            Long id,
            ScreenSummary screen,
            String rowLabel,
            Integer seatNumber,
            SeatType seatType,
            BigDecimal basePrice,
            boolean active
    ) {
        this.id = id;
        this.screen = screen;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.basePrice = basePrice;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScreenSummary getScreen() {
        return screen;
    }

    public void setScreen(ScreenSummary screen) {
        this.screen = screen;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
