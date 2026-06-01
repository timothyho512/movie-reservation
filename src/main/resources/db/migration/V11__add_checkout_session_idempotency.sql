ALTER TABLE checkout_sessions
    ADD COLUMN idempotency_key VARCHAR(255),
    ADD COLUMN idempotency_request_fingerprint VARCHAR(255);

CREATE UNIQUE INDEX uk_checkout_sessions_user_idempotency_key
    ON checkout_sessions (user_id, idempotency_key)
    WHERE user_id IS NOT NULL
    AND idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX uk_checkout_sessions_guest_idempotency_key
    ON checkout_sessions (guest_email, guest_session_id, idempotency_key)
    WHERE guest_email IS NOT NULL
    AND guest_session_id IS NOT NULL
    AND idempotency_key IS NOT NULL;
