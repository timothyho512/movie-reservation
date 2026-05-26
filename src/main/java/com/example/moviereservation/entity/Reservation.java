package com.example.moviereservation.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;  // Who made the booking

    @Column(name = "guest_email")
    private String guestEmail;  // For guest bookings without a user account

    @ManyToOne
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;  // Which showtime was booked

//    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<ReservationSeat> seats = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "reservation_seats",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "seat_id")
    )
    private List<Seat> seats;

    @Column(nullable = false, unique = true)
    private String bookingReference;  // e.g., "BK20260204001"

    @Column(nullable = false)
    private Integer numberOfSeats;  // How many seats

    @Column(nullable = false)
    private BigDecimal totalPrice;  // Total amount paid

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;  // PENDING, CONFIRMED, CANCELLED, COMPLETED, if pass showtime, then it is COMPLETED

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;  // PENDING, PAID, REFUNDED, FAILED

    @Column(nullable = false)
    private LocalDateTime bookingTime;  // When booking was made

    private LocalDateTime cancelledAt;  // If cancelled, when?

    private LocalDateTime updatedAt;

    public Reservation() {}

    public Reservation(User user, String guestEmail, Showtime showtime, List<Seat> seats, String bookingReference, BigDecimal totalPrice) {
        this.user = user;
        this.guestEmail = guestEmail;
        this.showtime = showtime;
        this.seats = seats;
        this.bookingReference = bookingReference;
        this.numberOfSeats = seats.size();
        this.totalPrice = totalPrice;
        this.currency = CurrencyCode.GBP;  // Default currency
        this.status = ReservationStatus.PENDING;
        this.paymentStatus = PaymentStatus.PENDING;
        this.bookingTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // helper function
    public boolean isGuestReservation() {
        return user == null && guestEmail != null;
    }

    public boolean isRegisteredUserReservation() {
        return user != null && guestEmail == null;
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

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
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

    public CurrencyCode getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyCode currency) {
        this.currency = currency;
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

    // the status column is important because if it is completed, then we might not want to tdelete this row immediately
    // at some point we might want to have a scheduled task to delete old completed reservations after a certain period of time, or we can just keep them for historical records and analytics purposes
    // and this could as well deleted the row in reservation_seats,
    
    // reservation_seats is a join table with column reservation_id and seat_id, 

}
