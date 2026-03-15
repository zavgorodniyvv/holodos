# Holodos

Production-oriented MVP platform for mobile-first home inventory and shopping management.

## Current status
This repository now includes **Phase 1 foundation**:
- backend bootstrap (Spring Boot modular monolith skeleton)
- initial Flyway migration for core catalog tables
- core catalog CRUD API for storage places, units, categories, stores, products
- global API error format + correlation-id support
- baseline security/cache/scheduling configuration
- unit tests for critical catalog business logic path (product creation dependencies)

## Tech stack
- Java 25
- Spring Boot 4
- PostgreSQL + Flyway
- Spring Data JPA, Validation, Security, OAuth2 Client
- Spring Cache with Caffeine
- OpenAPI/Swagger
- JUnit5 + Mockito + Testcontainers (dependencies enabled)

## Architecture documents
- Detailed architecture and phased plan: `docs/architecture.md`

## Run locally
1. Start PostgreSQL:
   ```bash
   docker compose up -d
   ```
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```
3. Open Swagger UI:
   - `http://localhost:8080/swagger-ui.html`

## Test
```bash
mvn test
```

## Implemented endpoints (initial)
- `POST/GET /api/storage-places`
- `POST/GET /api/units`
- `POST/GET /api/categories`
- `POST/GET /api/stores`
- `POST/GET /api/products` (`query` search parameter)

## Next planned steps
- implement inventory module (`stock_entries`, movements, adjustments)
- implement shopping list + purchase processing + auto-replenishment dedup
- add operation log writes on domain events
- scaffold Flutter app with offline SQLite and sync queue
- build Google Keep adapter contracts + stub sync orchestrator
Production-friendly MVP baseline for a mobile-first home inventory + shopping management system.

## Implemented in this increment
- Architecture and domain design document (`docs/architecture.md`)
- Spring Boot backend bootstrap (`backend/`)
- Flyway migrations for catalog dictionaries/products + operation log
- Core catalog module CRUD APIs for:
  - Storage places (flat)
  - Units
  - Categories
  - Stores
  - Products

- Inventory module baseline (`/api/stock-entries`)
- Shopping list module baseline (`/api/shopping-list`)
- Purchase processing baseline (`/api/purchases/process`)
- Movement history endpoint (`/api/movements`)
- Operation log hooks for inventory/shopping/purchase actions
- Filtering + pagination on inventory/shopping/movements list APIs
- Google Keep integration boundary with stub adapter and sync state persistence
- Notification/settings APIs + scheduled expiry/old-item checks
- Reports API (`/api/reports`) + CSV export endpoint (`/api/reports/export`)
- JSON backup/export and restore API (`/api/export/json`, `/api/export/restore`)
- Global validation error format + correlation ID support
- Basic unit test for product creation business rule

## Tech stack
- Java 25
- Spring Boot 4 (milestone)
- PostgreSQL + Flyway
- Spring Data JPA
- Spring Validation
- Spring Security
- OAuth2 Client (enabled in baseline config)
- Spring Cache + Caffeine
- OpenAPI (springdoc)
- JUnit 5 + Mockito + Testcontainers dependencies

## Run backend
```bash
cd backend
mvn spring-boot:run
```

Default DB connection:
- url: `jdbc:postgresql://localhost:5432/holodos`
- username: `holodos`
- password: `holodos`

Swagger UI:
- `http://localhost:8080/swagger-ui.html`

## Useful API examples
```bash
curl -X POST http://localhost:8080/api/storage-places \
  -H 'Content-Type: application/json' \
  -d '{"name":"Bathroom","description":"Bath area","icon":"bath","color":"#22A","sortOrder":40,"active":true}'

curl "http://localhost:8080/api/products?search=milk&page=0&size=20"
```

## Test
```bash
cd backend
mvn test
```

## Next steps
- Real Google Keep adapter behind existing integration interface
- Enhanced sync retry/diagnostics + Keep inbound processing
- CSV/JSON import coverage extension (media, stock history, notifications)
- JSON export + backup/restore endpoints
- Real Google Keep adapter behind existing integration interface
- Enhanced sync retry/diagnostics + Keep inbound processing
- Operation log writing from services
- Filtering/sorting/pagination for inventory, shopping and movement views
- Notifications + reports modules
- Notifications + reports modules
- CSV/JSON import-export + backup/restore
- Google Keep integration adapter + sync state/retry
- Reports module + CSV/JSON exports
- Real Google Keep adapter behind existing integration interface
- Enhanced sync retry/diagnostics + Keep inbound processing
- Inventory / stock entries module
- Shopping list + purchase processing
- Operation log writing from services
- Flutter mobile scaffold + SQLite sync queue
