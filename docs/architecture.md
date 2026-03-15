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
- Inventory tables: `stock_entries`, `inventory_transactions`, `movements`
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
