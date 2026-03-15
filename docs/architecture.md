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
