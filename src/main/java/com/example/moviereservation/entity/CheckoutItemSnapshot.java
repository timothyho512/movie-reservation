package com.example.moviereservation.entity;

import java.math.BigDecimal;

public class CheckoutItemSnapshot {
    private Long seatId;
    private String seatRow;
    private Integer seatNumber;
    private String seatType;
    private BigDecimal unitPrice;

    public CheckoutItemSnapshot() {
    }

    public CheckoutItemSnapshot(Long seatId, String seatRow, Integer seatNumber, String seatType, BigDecimal unitPrice) {
        this.seatId = seatId;
        this.seatRow = seatRow;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.unitPrice = unitPrice;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(String seatRow) {
        this.seatRow = seatRow;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
