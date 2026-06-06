package com.example.moviereservation.dto;

import com.example.moviereservation.entity.SeatType;

import java.math.BigDecimal;
import java.util.List;

public class AdminSeatLayoutRequest {
    private List<SeatDefinition> seats;

    public List<SeatDefinition> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatDefinition> seats) {
        this.seats = seats;
    }

    public static class SeatDefinition {
        private String rowLabel;
        private Integer seatNumber;
        private SeatType seatType;
        private BigDecimal basePrice;

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
}
