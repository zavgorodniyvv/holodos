CREATE TABLE media_objects (
    id BIGSERIAL PRIMARY KEY,
    object_key VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(200) NOT NULL,
    original_filename VARCHAR(255),
    size_bytes BIGINT NOT NULL,
    storage_provider VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_media_objects_created_at ON media_objects(created_at DESC);
