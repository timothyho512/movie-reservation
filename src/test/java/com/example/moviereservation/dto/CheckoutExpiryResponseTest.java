package com.example.moviereservation.dto;

import com.example.moviereservation.entity.CheckoutSessionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutExpiryResponseTest {

    @Test
    void lockResponseAddsServerOffsetToExpiry() {
        LocalDateTime expiry = LocalDateTime.of(2026, 6, 13, 12, 30);
        CheckoutLockResponse response = new CheckoutLockResponse(
                "session",
                expiry,
                List.of(1L),
                "ok"
        );

        assertThat(response.getExpiresAtWithOffset())
                .isEqualTo(expiry.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        assertThat(response.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void checkoutSessionResponseAddsServerOffsetToExpiry() {
        LocalDateTime expiry = LocalDateTime.of(2026, 6, 13, 12, 30);
        CheckoutSessionCreateResponse response = new CheckoutSessionCreateResponse(
                "checkout",
                "stripe-session",
                "https://checkout.stripe.test",
                CheckoutSessionStatus.PENDING_PAYMENT,
                expiry,
                "ok"
        );

        assertThat(response.getExpiresAtWithOffset())
                .isEqualTo(expiry.atZone(ZoneId.systemDefault()).toOffsetDateTime());
        assertThat(response.getExpiresAt()).isEqualTo(expiry);
    }
}
