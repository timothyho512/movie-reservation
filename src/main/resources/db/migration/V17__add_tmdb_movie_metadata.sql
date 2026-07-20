ALTER TABLE movie
    ADD COLUMN tmdb_id BIGINT,
    ADD COLUMN poster_path VARCHAR(255),
    ADD COLUMN overview VARCHAR(2000),
    ADD COLUMN release_date DATE,
    ADD COLUMN runtime_minutes INTEGER,
    ADD COLUMN tmdb_managed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN now_playing BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN last_synced_at TIMESTAMP;

CREATE UNIQUE INDEX uk_movie_tmdb_id ON movie (tmdb_id) WHERE tmdb_id IS NOT NULL;
CREATE INDEX idx_movie_now_playing_active ON movie (now_playing, active);
