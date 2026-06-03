package com.example.moviereservation.repository;

import com.example.moviereservation.entity.CheckoutLockIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CheckoutLockIdempotencyKeyRepository extends JpaRepository<CheckoutLockIdempotencyKey, Long> {

    Optional<CheckoutLockIdempotencyKey> findFirstByUserIdAndIdempotencyKey(
            Long userId,
            String idempotencyKey
    );

    Optional<CheckoutLockIdempotencyKey> findFirstByGuestEmailAndIdempotencyKey(
            String guestEmail,
            String idempotencyKey
    );
}
