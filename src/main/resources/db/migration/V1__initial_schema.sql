CREATE TABLE storage_places (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(64),
    color VARCHAR(32),
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE units (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    short_name VARCHAR(20) NOT NULL,
    unit_type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(64),
    color VARCHAR(32),
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE stores (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(64),
    color VARCHAR(32),
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    category_id UUID NOT NULL REFERENCES categories(id),
    default_unit_id UUID NOT NULL REFERENCES units(id),
    default_storage_place_id UUID NOT NULL REFERENCES storage_places(id),
    default_store_id UUID REFERENCES stores(id),
    photo_url VARCHAR(500),
    description VARCHAR(1000),
    shelf_life_days INT,
    minimum_quantity_threshold NUMERIC(12,3),
    reorder_quantity NUMERIC(12,3),
    auto_add_to_shopping_list BOOLEAN NOT NULL DEFAULT TRUE,
    barcode VARCHAR(64),
    note VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE operation_logs (
    id UUID PRIMARY KEY,
    action_type VARCHAR(60) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID,
    payload JSONB,
    correlation_id VARCHAR(80),
    actor VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_storage_places_active ON storage_places(active);
CREATE INDEX idx_categories_active ON categories(active);
CREATE INDEX idx_stores_active ON stores(active);
