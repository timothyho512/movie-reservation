package com.example.moviereservation.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat_locks")
public class SeatLock {
    private static final int DEFAULT_LOCK_DURATION_MINUTES = 15; // there could be a better way to do this, for example, put this in a config file, e.g. application.properties

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;  // Who locked it (optional, or use session ID)

    @Column(name = "session_id")
    private String sessionId;  // Alternative to user for anonymous sessions

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(nullable = false)
    private LocalDateTime lockedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // e.g., 10-15 minutes from now

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LockStatus status;  // Audit/history status. Redis is the active hold authority.

//    private Long reservationId;  // Link to reservation if converted

    // Constructors
    public SeatLock() {
    }

    public SeatLock(Seat seat, Showtime showtime, User user, String sessionId, String guestEmail) {
        this.seat = seat;
        this.showtime = showtime;
        this.user = user;
        this.sessionId = sessionId;
        this.guestEmail = guestEmail;
    }

    @PrePersist
    public void prePersist() {
        if (lockedAt == null) {
            lockedAt = LocalDateTime.now();
        }

        if (expiresAt == null) {
            expiresAt = lockedAt.plusMinutes(DEFAULT_LOCK_DURATION_MINUTES);
        }

        if (status == null) {
            status = LockStatus.LOCKED;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LockStatus getStatus() {
        return status;
    }

    public void setStatus(LockStatus status) {
        this.status = status;
    }


    // Helper function
    // this helper function is the "Business Logic" inside the entity where it belongs
    public boolean isExpired() {
        return status == LockStatus.EXPIRED || LocalDateTime.now().isAfter(expiresAt);
    }
}
/**
 * Redis is now the active source of truth for temporary seat holds.
 *
 * This entity is kept as an audit/history record so admins and developers can
 * inspect lock attempts and their final state. It should not be used to decide
 * whether a seat is currently available.
 */
