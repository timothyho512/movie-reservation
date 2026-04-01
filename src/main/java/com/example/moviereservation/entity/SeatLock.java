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

    @Column(nullable = false)
    private LocalDateTime lockedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // e.g., 10-15 minutes from now

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LockStatus status;  // LOCKED, EXPIRED, CONVERTED_TO_RESERVATION

//    private Long reservationId;  // Link to reservation if converted

    // Constructors
    public SeatLock() {
    }

    public SeatLock(Seat seat, Showtime showtime, User user, String sessionId) {
        this.seat = seat;
        this.showtime = showtime;
        this.user = user;
        this.sessionId = sessionId;

        this.lockedAt = LocalDateTime.now();
        this.expiresAt = this.lockedAt.plusMinutes(DEFAULT_LOCK_DURATION_MINUTES);  // Default expiration time
        this.status = LockStatus.LOCKED;
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
 * Lesson Learn
 * - Normal unique indexes stop any duplicate. But here, you want to allow a duplicate if the old lock is expired.
 * - This index says: "You cannot have two rows for the same seat/showtime IF they are both currently "LOCKED"."
 * - If someone tries to insert a second lock for a seat that is already locked, the database will throw an error, preventing the double-booking at the hardware level.
 * The Collision: If the Database Shield (above) triggers, your Java code catches a DataIntegrityViolationException.
 * This is called partial unique index
 */
// IMPORTANT: need partial unique index ON seat_locks (seat_id, showtime_id)
// WHERE status IN ('LOCKED', 'CONVERTED_TO_RESERVATION');

// When user clicks checkout: Create SeatLock entries for the selected seats + showtime
// If another user already locked it, the insert fails; catch the unique‑violation and return “seat
// unavailable”.
// Check availability: Query for non-expired locks AND confirmed reservations
// Lock expiration: Set expiresAt to 10-15 minutes from lockedAt
// On successful payment: Convert lock to Reservation, update SeatLock.status
// Cleanup: Periodically remove expired locks (scheduled job or on-demand)
/**
 * The "Garbage Collection" (Cleanup)
 * - Scheduled Job: A "Spring Cron" task that runs every minute: DELETE FROM seat_locks WHERE expiresAt < NOW() AND status = "LOCKED".
 * - On-Demand: Every time a user searches for seats, your first run a query to clear out any expired ones so those seats appear "available" again
 */

//
 // If expired, then we update row as expired
 // and clean up if we want
