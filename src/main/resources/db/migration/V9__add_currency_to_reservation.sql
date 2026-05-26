ALTER TABLE reservation
    ADD COLUMN currency VARCHAR(3);

UPDATE reservation
SET currency = 'GBP'
WHERE currency IS NULL;

ALTER TABLE reservation
    ALTER COLUMN currency SET NOT NULL;

ALTER TABLE reservation
    ALTER COLUMN currency SET DEFAULT 'GBP';