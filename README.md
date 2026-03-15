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
