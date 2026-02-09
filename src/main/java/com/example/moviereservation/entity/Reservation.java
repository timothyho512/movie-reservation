package com.example.moviereservation.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // Who made the booking

    @ManyToOne
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;  // Which showtime was booked

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationSeat> seats = new ArrayList<>();

    @Column(nullable = false, unique = true)
    private String bookingReference;  // e.g., "BK20260204001"

    @Column(nullable = false)
    private Integer numberOfSeats;  // How many seats

    @Column(nullable = false)
    private BigDecimal totalPrice;  // Total amount paid

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;  // PENDING, CONFIRMED, CANCELLED, COMPLETED

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;  // PENDING, PAID, REFUNDED, FAILED

    @Column(nullable = false)
    private LocalDateTime bookingTime;  // When booking was made

    private LocalDateTime cancelledAt;  // If cancelled, when?

    private LocalDateTime updatedAt;

    public Reservation() {}

    public Reservation(User user, Showtime showtime, List<ReservationSeat> seats, String bookingReference, BigDecimal totalPrice) {
        this.user = user;
        this.showtime = showtime;
        this.seats = seats;
        this.bookingReference = bookingReference;
        this.numberOfSeats = seats.size();
        this.totalPrice = totalPrice;
        this.status = ReservationStatus.PENDING;
        this.paymentStatus = PaymentStatus.PENDING;
        this.bookingTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public List<ReservationSeat> getSeats() {
        return seats;
    }

    public void setSeats(List<ReservationSeat> seats) {
        this.seats = seats;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public Integer getNumberOfSeats() {
        return numberOfSeats;
    }

    public void setNumberOfSeats(Integer numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
