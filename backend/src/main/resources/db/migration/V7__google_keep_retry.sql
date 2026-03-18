ALTER TABLE sync_bindings
    ADD COLUMN last_error_message VARCHAR(1000),
    ADD COLUMN failure_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMPTZ,
    ADD COLUMN last_sync_status VARCHAR(32);
