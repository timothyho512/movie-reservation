ALTER TABLE checkout_sessions
    ADD COLUMN stripe_refund_id VARCHAR(255),
    ADD COLUMN refunded_at TIMESTAMP,
    ADD COLUMN refund_error VARCHAR(2000);

CREATE INDEX idx_showtime_status_start_end ON showtime (status, start_time, end_time);
CREATE INDEX idx_reservation_showtime_status_payment ON reservation (showtime_id, status, payment_status);
