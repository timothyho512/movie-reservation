-- V4__include_processing_in_seat_lock_index.sql
DROP INDEX IF EXISTS ux_seat_lock_active;

CREATE UNIQUE INDEX ux_seat_lock_active
    ON seat_locks (seat_id, showtime_id)
    WHERE status IN ('LOCKED', 'PROCESSING', 'CONVERTED_TO_RESERVATION');
