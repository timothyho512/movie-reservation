ALTER TABLE checkout_lock_idempotency_keys
    ADD COLUMN status VARCHAR(255);

UPDATE checkout_lock_idempotency_keys
    SET status = 'COMPLETED'
    WHERE status IS NULL;

ALTER TABLE checkout_lock_idempotency_keys
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN expires_at DROP NOT NULL,
    ALTER COLUMN locked_seat_ids DROP NOT NULL;

ALTER TABLE checkout_lock_idempotency_keys
    ADD COLUMN last_error TEXT;

CREATE INDEX idx_checkout_lock_idempotency_status
    ON checkout_lock_idempotency_keys (status);
