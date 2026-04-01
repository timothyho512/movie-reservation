CREATE UNIQUE INDEX ux_seat_lock_active
    ON seat_locks (seat_id, showtime_id)
    WHERE status IN ('LOCKED', 'CONVERTED_TO_RESERVATION');