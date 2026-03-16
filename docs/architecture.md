# Holodos MVP Architecture (Phase 1 baseline)

## 1) Architecture overview
- **Style:** Modular monolith.
- **Backend:** Java 25 + Spring Boot 4, REST API, PostgreSQL, Flyway, JPA, Validation, Security, Cache (Caffeine), Scheduler.
- **Mobile:** Flutter (planned in `mobile/` in next step) with Riverpod + SQLite + sync queue.
- **Integration:** Google Keep isolated behind integration adapter contracts (`integrations` module planned in Phase 3).
- **Storage places:** strictly flat dictionary; no hierarchy.

## 2) Module/package structure
Each module follows:
- `api` (controllers + transport DTO)
- `application` (use cases / orchestration)
- `domain` (entities, domain rules)
- `infrastructure` (repositories, external adapters)

Planned modules:
- `auth`, `catalog`, `inventory`, `shopping`, `stores`, `notifications`, `reports`, `integrations`, `media`, `settings`, `common`.

Current implementation:
- `common` (error model, correlation ID filter, cache/security config, base entity)
- `catalog` (storage places, units, categories, stores, products CRUD)

## 3) Domain model summary
Core entities for full MVP:
- `StoragePlace`, `UnitOfMeasure`, `Category`, `Store`, `Product`
- `StockEntry`, `ShoppingListItem`, `PurchaseEvent`, `InventoryTransaction`
- `Notification`, `SyncBinding`, `SyncEvent`, `UserSettings`

Current phase implemented:
- `StoragePlace`, `UnitOfMeasure`, `Category`, `Store`, `Product`
- plus `operation_log` table foundation.

## 4) Database schema proposal
Implemented migrations include:
- dictionaries: `storage_places`, `units`, `categories`, `stores`
- catalog table: `products`
- audit table: `operation_log`
- seed data for baseline dictionaries.

Next migrations will add:
- `notifications`, `sync_bindings`, `sync_events`, `user_settings`
- reports materialization and export snapshots
- integration diagnostics tables.

## 5) API proposal
Implemented endpoints:
- `GET/POST/PUT/DELETE /api/storage-places`
- `GET/POST/PUT/DELETE /api/units`
- `GET/POST/PUT/DELETE /api/categories`
- `GET/POST/PUT/DELETE /api/stores`
- `GET/POST/PUT/DELETE /api/products`

Cross-cutting:
- validation via Bean Validation
- standard error payload (`ErrorResponse`)
- correlation id support via `X-Correlation-Id`
- pagination for product listing (`Pageable`)

Planned endpoints in next phases:
- `/api/notifications`, `/api/reports`, `/api/settings`, `/api/integrations/google-keep`.

## 6) Mobile app structure (planned)
Flutter feature-first structure:
- `lib/core` (network, offline db, sync queue, localization, theme)
- `lib/features/dashboard`
- `lib/features/catalog`
- `lib/features/inventory`
- `lib/features/shopping`
- `lib/features/notifications`
- `lib/features/reports`
- `lib/features/settings`
- `lib/features/integrations/google_keep`

Screens planned:
- Dashboard, Products, Product detail, Storage places, Inventory, Shopping list,
  Stores, Notifications, Reports, Settings, Google Keep settings.

## 7) Phased delivery plan
### Phase 1
- backend skeleton
- base schema + flyway
- catalog CRUD
- initial inventory/shopping/purchase flows
- operation log baseline
- mobile scaffold with offline DB skeleton

### Phase 2
- notification jobs and thresholds
- export/import CSV+JSON
- sync queue hardening
- dashboard and reports upgrades

### Phase 3
- real Google Keep adapter
- idempotent bi-directional sync
- retry, diagnostics, and integration error observability

## Increment 2 (implemented)
- Added `inventory` module baseline:
  - stock entry CRUD-like flows (`add`, `consume`, `discard`, `move`)
  - movement log persistence with flat storage-place constraints
- Added `shopping` module baseline:
  - shopping list item create/update/list
  - completion flow
  - auto-add replenishment with active-item deduplication by product
- Added `purchases` module baseline:
  - process shopping item as purchased
  - create `purchase_event`
  - create stock entry from purchase
  - infer expiry from product shelf-life when omitted

- Added dedicated `/api/movements` read endpoint for movement history.


## Increment 3 (implemented)
- Added operation/audit log service and logging hooks for key flows:
  - stock add/consume/discard/move
  - shopping create/update/complete/auto-add
  - purchase processing
- Added filtering + pagination support for:
  - `/api/stock-entries` (status, storage place, search)
  - `/api/shopping-list` (status, store, search)
  - `/api/movements` (from/to storage place)


## Increment 4 (implemented)
- Added Google Keep integration boundary module (`integrations/googlekeep`):
  - adapter interface `GoogleKeepClient`
  - stub implementation `StubGoogleKeepClient`
  - bind + sync orchestration service with sync binding/event persistence
  - REST endpoints at `/api/integrations/google-keep`
- Added notifications/settings foundation:
  - `user_settings`, `notifications`, `sync_bindings`, `sync_events` migration
  - settings API at `/api/settings`
  - notifications API at `/api/notifications`
  - scheduled inventory checks for expiring soon + stored too long notifications


## Increment 5 (implemented)
- Added `reports` module baseline with REST endpoints:
  - `/api/reports` (aggregated metrics with filters)
  - `/api/reports/export` (CSV export)
- Implemented report aggregations for:
  - inventory by storage place/category
  - shopping by store
  - operation log by event type
  - expiring/expired/stored-too-long counts
  - purchases/discards counters


## Increment 6 (implemented)
- Added `export` module baseline with backup/restore endpoints:
  - `/api/export/json` and `/api/export/json/download`
  - `/api/export/restore`
- Implemented JSON snapshot for dictionaries/products/shopping items and controlled restore flow.
- Added initial unit tests for export and restore service behavior.


## Increment 7 (implemented)
- Extended Google Keep sync orchestration with inbound processing:
  - checked Keep checklist items are mapped to shopping item ids (`shopping-{id}`)
  - checked items are processed via purchase flow
  - inbound sync events are persisted with idempotency keys
- Added retry capabilities:
  - service method to retry last failed sync per user
  - scheduled retry job across enabled bindings
  - new API endpoints `/api/integrations/google-keep/sync-inbound` and `/retry-failed`


## Increment 8 (implemented)
- Extended `export` module with dataset CSV endpoints:
  - `/api/export/csv/products`
  - `/api/export/csv/shopping-list`
  - `/api/export/csv/operation-log`
- Added `CsvExportService` and unit tests for products/shopping/operation-log CSV generation.

## Increment 9 (implemented)
- Added `media` module baseline for product photos with clean storage adapter boundary:
  - `MediaStorageGateway` abstraction for pluggable providers
  - filesystem adapter implementation for local/dev environments
- Added product photo API:
  - `POST /api/products/{id}/photo` (multipart upload/replace)
  - `GET /api/products/{id}/photo` (photo download)
  - `DELETE /api/products/{id}/photo` (remove photo)
- Added `media_objects` Flyway migration and metadata persistence while keeping `products.photo_key` as the domain link.

## Increment 10 (implemented)
- Extended media infrastructure with configurable provider selection:
  - `holodos.media.provider=FILESYSTEM|S3`
  - runtime gateway wiring in `MediaStorageConfig`
- Added `S3MediaStorageGateway` for S3-compatible object storage backends (AWS S3/MinIO-style endpoints).
- Hardened filesystem gateway path resolution to prevent path traversal outside configured media root.

## Increment 11 (implemented)
- Hardened media upload flow:
  - whitelist content types (`image/jpeg`, `image/png`, `image/webp`)
  - max upload size of 10MB
  - reject empty payloads
- Added cleanup compensation in `MediaService` to remove newly stored objects when DB persistence fails during upload.
- Replaced hardcoded S3 sample credentials in runtime config with environment-variable placeholders.


## Increment 12 (implemented)
- Improved media failure handling and safety:
  - S3 gateway validates required non-blank credentials/region and cleanly closes S3 client on bean destroy
  - media upload tests now cover empty/oversized files and compensation cleanup on persistence failure
