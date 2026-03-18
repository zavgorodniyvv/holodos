# Holodos MVP Architecture (Phase-oriented)

## 1) Architecture overview
- **Style**: modular monolith with clear module boundaries (`catalog`, `inventory`, `shopping`, `notifications`, `reports`, `integrations`, etc.).
- **Backend**: Java 25 + Spring Boot 4, REST API, PostgreSQL, Flyway, Spring Data JPA, Spring Validation, Spring Security/OAuth2 Client, Scheduler, Cache (Caffeine).
- **Mobile**: Flutter (Riverpod), offline-first with SQLite + sync queue.
- **Integration boundary**: Google Keep isolated behind adapter interface in `integrations` module.
- **Storage**: PostgreSQL + S3-compatible media storage for photos.

## 2) Module/package structure
Each module follows:
- `api` – controllers, HTTP DTOs
- `application` – use cases/services
- `domain` – aggregates, rules, repository interfaces
- `infrastructure` – persistence adapters, external adapters, config

Main modules:
- `auth`
- `catalog`
- `inventory`
- `shopping`
- `stores`
- `notifications`
- `reports`
- `integrations`
- `media`
- `settings`
- `common`

## 3) Domain model summary
Primary entities:
- `StoragePlace` (flat list only)
- `UnitOfMeasure`
- `Category`
- `Store`
- `Product`
- `StockEntry`
- `ShoppingListItem`
- `PurchaseEvent`
- `InventoryTransaction`
- `InventoryAdjustment`
- `Notification`
- `SyncBinding`
- `SyncEvent`
- `UserSettings`

Core rules:
- storage places are flat, never hierarchical
- product is a template; stock entries are concrete batches
- auto-replenishment deduplicates active shopping entries
- movement cannot produce negative stock and requires active source/target places
- Keep sync must be idempotent and avoid circular updates

## 4) Database schema proposal (MVP to full)
- Dictionary tables: `storage_places`, `units`, `categories`, `stores`
- Catalog table: `products`
- Inventory tables: `stock_entries`, `inventory_transactions`, `movements`, `inventory_adjustments`
- Shopping tables: `shopping_list_items`, `purchase_events`
- Notification tables: `notifications`, `notification_settings`
- Integration tables: `sync_bindings`, `sync_events`
- Ops tables: `operation_logs`

## 5) API proposal
Base endpoints:
- `/api/storage-places`
- `/api/units`
- `/api/categories`
- `/api/stores`
- `/api/products`
- `/api/stock-entries`
- `/api/shopping-list`
- `/api/purchases`
- `/api/movements`
- `/api/reports`
- `/api/notifications`
- `/api/settings`
- `/api/integrations/google-keep`

Cross-cutting API behavior:
- JSON, pagination/filtering/sorting
- validation errors in standard envelope
- optimistic locking using `version`
- correlation id in `X-Correlation-Id`

## 6) Mobile app structure proposal
Flutter features/modules:
- `features/dashboard`
- `features/products`
- `features/storage_places`
- `features/inventory`
- `features/shopping_list`
- `features/stores`
- `features/notifications`
- `features/reports`
- `features/settings`
- `features/google_keep`

Layers:
- presentation (widgets/screens)
- application (providers/use-cases)
- data (API + SQLite + sync queue)
- domain (entities/value objects)

## 7) Delivery plan
- **Phase 1**: backend skeleton, Flyway, core dictionaries/catalog CRUD, stock/shopping core flows, purchase processing, operation log baseline, basic reports, Flutter screen scaffolds.
- **Phase 2**: notification jobs, expiry/old-item checks, export/import, richer dashboard, offline sync queue improvements.
- **Phase 3**: real Google Keep adapter, retry/idempotency/circular-sync guards, diagnostics and polish.
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
- `common` (error model, correlation ID filter, cache/security config, base entity, domain event publisher + operation log listener)
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
- `stock_entries`, `shopping_list_items`, `purchase_events`, `inventory_transactions`
- notification and sync state tables
- settings and export snapshots.


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
- `/api/stock-entries`, `/api/shopping-list`, `/api/purchases`, `/api/movements`
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
