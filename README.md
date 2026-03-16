# Holodos

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
- Google Keep inbound sync + retry endpoints (`/api/integrations/google-keep/sync-inbound`, `/retry-failed`)
- Notification/settings APIs + scheduled expiry/old-item checks
- Reports API (`/api/reports`) + CSV export endpoint (`/api/reports/export`)
- JSON backup/export and restore API (`/api/export/json`, `/api/export/restore`)
- Dataset CSV export endpoints (`/api/export/csv/products`, `/api/export/csv/shopping-list`, `/api/export/csv/operation-log`)
- Product photo media API with storage adapter boundary (`/api/products/{id}/photo`)
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
- Real Google Keep adapter behind existing integration interface (OAuth tokens + API wiring)
- S3-compatible media storage adapter implementation (currently filesystem adapter)
- CSV/JSON import coverage extension (stock history, notifications)
- Flutter mobile scaffold + SQLite sync queue
- Tablet/mobile UI + offline sync queue implementation
