CREATE TABLE google_tasks_bindings (
    id              BIGSERIAL PRIMARY KEY,
    user_key        VARCHAR(255) NOT NULL UNIQUE,
    task_list_id    VARCHAR(255),
    access_token    TEXT,
    refresh_token   TEXT,
    token_expires_at TIMESTAMPTZ,
    enabled         BOOLEAN NOT NULL DEFAULT true,
    last_synced_at  TIMESTAMPTZ,
    last_sync_status VARCHAR(50),
    failure_count   INT NOT NULL DEFAULT 0,
    last_error_message TEXT,
    next_retry_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0
);
