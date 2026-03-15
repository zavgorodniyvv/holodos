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
- Notifications + reports modules
- CSV/JSON import-export + backup/restore
- Google Keep integration adapter + sync state/retry
- Flutter mobile scaffold + SQLite sync queue
