ALTER TABLE reservation
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE reservation
    ADD COLUMN guest_email VARCHAR(255);

ALTER TABLE reservation
    ADD CONSTRAINT chk_reservation_identity
    CHECK (
        (user_id IS NOT NULL AND guest_email IS NULL)
        OR
        (user_id IS NULL AND guest_email IS NOT NULL)
    );

ALTER TABLE seat_locks
    ADD CONSTRAINT chk_seat_locks_identity
    CHECK (
        (user_id IS NOT NULL AND session_id IS NULL)
        OR
        (user_id IS NULL AND session_id IS NOT NULL)
    );
