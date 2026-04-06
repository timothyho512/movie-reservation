ALTER TABLE seat_locks
    ADD COLUMN guest_email VARCHAR(255);

ALTER TABLE seat_locks
    DROP CONSTRAINT IF EXISTS chk_seat_locks_identity;

ALTER TABLE seat_locks
    ADD CONSTRAINT chk_seat_locks_identity
    CHECK (
        (user_id IS NOT NULL AND session_id IS NULL AND guest_email IS NULL)
        OR
        (user_id IS NULL AND session_id IS NOT NULL AND guest_email IS NOT NULL)
    );