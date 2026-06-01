package com.example.moviereservation.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkout_sessions")
public class CheckoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String checkoutReference;

    @ManyToOne
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_session_id")
    private String guestSessionId;

    @Lob
    @Column(name = "items_snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String itemsSnapshotJson;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckoutSessionStatus status;

    @Column(name = "stripe_checkout_session_id", unique = true)
    private String stripeCheckoutSessionId;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "stripe_customer_email")
    private String stripeCustomerEmail;

    @Column(name = "checkout_url", length = 2000)
    private String checkoutUrl;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "idempotency_request_fingerprint")
    private String idempotencyRequestFingerprint;


    @OneToOne
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    private LocalDateTime failedAt;

    private LocalDateTime cancelledAt;

    public CheckoutSession() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = CheckoutSessionStatus.PENDING_PAYMENT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isGuestCheckout() {
        return user == null
                && guestEmail != null && !guestEmail.isBlank()
                && guestSessionId != null && !guestSessionId.isBlank();
    }

    public boolean isAuthenticatedCheckout() {
        return user != null && guestEmail == null && guestSessionId == null;
    }

    public Long getId() {
        return id;
    }

    public String getCheckoutReference() {
        return checkoutReference;
    }

    public void setCheckoutReference(String checkoutReference) {
        this.checkoutReference = checkoutReference;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
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

    public String getGuestSessionId() {
        return guestSessionId;
    }

    public void setGuestSessionId(String guestSessionId) {
        this.guestSessionId = guestSessionId;
    }

    public String getItemsSnapshotJson() {
        return itemsSnapshotJson;
    }

    public void setItemsSnapshotJson(String itemsSnapshotJson) {
        this.itemsSnapshotJson = itemsSnapshotJson;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyCode currency) {
        this.currency = currency;
    }

    public CheckoutSessionStatus getStatus() {
        return status;
    }

    public void setStatus(CheckoutSessionStatus status) {
        this.status = status;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStripeCustomerEmail() {
        return stripeCustomerEmail;
    }

    public void setStripeCustomerEmail(String stripeCustomerEmail) {
        this.stripeCustomerEmail = stripeCustomerEmail;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyRequestFingerprint() {
        return idempotencyRequestFingerprint;
    }

    public void setIdempotencyRequestFingerprint(String idempotencyRequestFingerprint) {
        this.idempotencyRequestFingerprint = idempotencyRequestFingerprint;
    }


    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
