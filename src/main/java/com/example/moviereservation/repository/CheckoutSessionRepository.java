package com.example.moviereservation.repository;

import com.example.moviereservation.entity.CheckoutSession;
import com.example.moviereservation.entity.CheckoutSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.moviereservation.entity.CheckoutSessionStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, Long> {

    Optional<CheckoutSession> findByCheckoutReference(String checkoutReference);

    Optional<CheckoutSession> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    Optional<CheckoutSession> findFirstByUserIdAndShowtimeIdAndItemsSnapshotJsonAndStatusAndExpiresAtAfter(
            Long userId,
            Long showtimeId,
            String itemsSnapshotJson,
            CheckoutSessionStatus status,
            LocalDateTime now
    );

    Optional<CheckoutSession> findFirstByGuestEmailAndGuestSessionIdAndShowtimeIdAndItemsSnapshotJsonAndStatusAndExpiresAtAfter(
            String guestEmail,
            String guestSessionId,
            Long showtimeId,
            String itemsSnapshotJson,
            CheckoutSessionStatus status,
            LocalDateTime now
    );

    List<CheckoutSession> findAllByStatusAndExpiresAtBefore(
            CheckoutSessionStatus status,
            LocalDateTime now
    );

    @Modifying
        @Query("""
                UPDATE CheckoutSession cs
                SET cs.status = com.example.moviereservation.entity.CheckoutSessionStatus.CANCELLED,
                        cs.cancelledAt = CURRENT_TIMESTAMP
                WHERE cs.showtime.id = :showtimeId
                AND cs.user.id = :userId
                AND cs.status = com.example.moviereservation.entity.CheckoutSessionStatus.PENDING_PAYMENT
        """)
        int cancelPendingSessionsForUser(
                @Param("showtimeId") Long showtimeId,
                @Param("userId") Long userId
        );

        @Modifying
        @Query("""
                UPDATE CheckoutSession cs
                SET cs.status = com.example.moviereservation.entity.CheckoutSessionStatus.CANCELLED,
                        cs.cancelledAt = CURRENT_TIMESTAMP
                WHERE cs.showtime.id = :showtimeId
                AND cs.guestSessionId = :sessionId
                AND cs.guestEmail = :guestEmail
                AND cs.status = com.example.moviereservation.entity.CheckoutSessionStatus.PENDING_PAYMENT
        """)
        int cancelPendingSessionsForGuest(
                @Param("showtimeId") Long showtimeId,
                @Param("sessionId") String sessionId,
                @Param("guestEmail") String guestEmail
        );

        @Modifying
        @Query("""
                UPDATE CheckoutSession cs
                SET cs.status = com.example.moviereservation.entity.CheckoutSessionStatus.EXPIRED
                WHERE cs.status = com.example.moviereservation.entity.CheckoutSessionStatus.PENDING_PAYMENT
                AND cs.expiresAt < :now
        """)
        int expireStalePendingSessions(@Param("now") LocalDateTime now);
}
