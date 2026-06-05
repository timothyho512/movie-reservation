package com.example.moviereservation.observability;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.example.moviereservation.Exception.SeatUnavailableException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class CheckoutMetrics {
    private final Counter checkoutLockAttempts;
    private final Counter checkoutLockSuccess;
    private final Counter checkoutLockConflicts;
    private final Counter checkoutSessionCreation;
    private final Counter checkoutSessionCreationFailures;
    private final Timer checkoutSessionCreationDuration;
    private final Counter paymentWebhookReceived;
    private final Counter paymentWebhookSuccess;
    private final Counter paymentWebhookFailure;
    private final Counter reservationFinalizationSuccess;
    private final Counter reservationFinalizationFailure;
    private final Counter expiredLocks;
    private final Counter expiredCheckoutSessions;

    public CheckoutMetrics(MeterRegistry meterRegistry) {
        this.checkoutLockAttempts = Counter.builder("checkout.lock.attempts")
                .description("Total checkout lock attempts")
                .register(meterRegistry);
        this.checkoutLockSuccess = Counter.builder("checkout.lock.success")
                .description("Total successful checkout lock attempts")
                .register(meterRegistry);
        this.checkoutLockConflicts = Counter.builder("checkout.lock.conflicts")
                .description("Total checkout lock attempts rejected by seat conflicts")
                .register(meterRegistry);
        this.checkoutSessionCreation = Counter.builder("checkout.session.creation")
                .description("Total checkout session creation attempts")
                .register(meterRegistry);
        this.checkoutSessionCreationFailures = Counter.builder("checkout.session.creation.failures")
                .description("Total failed checkout session creation attempts")
                .register(meterRegistry);
        this.checkoutSessionCreationDuration = Timer.builder("checkout.session.creation.duration")
                .description("Checkout session creation duration")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.paymentWebhookReceived = Counter.builder("payment.webhook.received")
                .description("Total payment webhooks received")
                .register(meterRegistry);
        this.paymentWebhookSuccess = Counter.builder("payment.webhook.success")
                .description("Total payment webhooks processed successfully")
                .register(meterRegistry);
        this.paymentWebhookFailure = Counter.builder("payment.webhook.failure")
                .description("Total payment webhook processing failures")
                .register(meterRegistry);
        this.reservationFinalizationSuccess = Counter.builder("reservation.finalization.success")
                .description("Total successful reservation finalizations")
                .register(meterRegistry);
        this.reservationFinalizationFailure = Counter.builder("reservation.finalization.failure")
                .description("Total failed reservation finalizations")
                .register(meterRegistry);
        this.expiredLocks = Counter.builder("expired.locks")
                .description("Total expired audit seat locks")
                .register(meterRegistry);
        this.expiredCheckoutSessions = Counter.builder("expired.checkout.sessions")
                .description("Total expired checkout sessions")
                .register(meterRegistry);
    }

    public void recordCheckoutLockAttempt() {
        checkoutLockAttempts.increment();
    }

    public void recordCheckoutLockSuccess() {
        checkoutLockSuccess.increment();
    }

    public void recordCheckoutLockFailure(RuntimeException exception) {
        if (exception instanceof SeatUnavailableException) {
            checkoutLockConflicts.increment();
        }
    }

    public <T> T recordCheckoutSessionCreation(Supplier<T> operation) {
        checkoutSessionCreation.increment();
        return checkoutSessionCreationDuration.record(operation);
    }

    public void recordCheckoutSessionCreationFailure() {
        checkoutSessionCreationFailures.increment();
    }

    public void recordPaymentWebhookReceived() {
        paymentWebhookReceived.increment();
    }

    public void recordPaymentWebhookSuccess() {
        paymentWebhookSuccess.increment();
    }

    public void recordPaymentWebhookFailure() {
        paymentWebhookFailure.increment();
    }

    public void recordReservationFinalizationSuccess() {
        reservationFinalizationSuccess.increment();
    }

    public void recordReservationFinalizationFailure() {
        reservationFinalizationFailure.increment();
    }

    public void recordExpiredLocks(int count) {
        expiredLocks.increment(count);
    }

    public void recordExpiredCheckoutSessions(int count) {
        expiredCheckoutSessions.increment(count);
    }
}
