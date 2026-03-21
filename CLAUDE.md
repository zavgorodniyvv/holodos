# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Holodos** — mobile-first home inventory and shopping management platform.

- `backend/` — Java 25 + Spring Boot 4 modular monolith REST API
- `mobile/` — Flutter (Dart) offline-first client with SQLite + sync
- `docs/` — Architecture and design documentation

## Running the Stack

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Start backend (port 8080)
cd backend && mvn spring-boot:run

# 3. Run mobile app
cd mobile && flutter run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Backend Commands

```bash
cd backend

mvn clean install                                    # Full build
mvn test                                             # All tests
mvn test -Dtest=ProductServiceTest                   # Single test class
mvn test -Dtest=ProductServiceTest#methodName        # Single test method
```

## Mobile Commands

```bash
cd mobile

flutter pub get    # Install dependencies
flutter run        # Run app
flutter test       # Run tests
flutter analyze    # Lint
```

## Backend Architecture

Each feature module follows a strict 4-layer structure:

```
com.holodos.<module>/
├── api/            # REST controllers + request/response DTOs (records)
├── application/    # Services, use cases, orchestration
├── domain/         # Entities, repository interfaces, domain rules
└── infrastructure/ # JPA repositories, external adapters, config
```

Dependency rule: `api` → `application` → `domain` ← `infrastructure`

**Modules:** `common`, `catalog`, `inventory`, `shopping`, `purchases`, `integrations.googlekeep`, `notifications`, `reports`, `export`, `media`, `settings`

### Cross-Cutting (`common`)
- `DomainEventPublisher` + `OperationLogEventListener` — all mutations emit domain events logged to `operation_log`
- `CorrelationIdFilter` — injects `X-Correlation-ID` on every request
- `GlobalExceptionHandler` — standardized `ErrorResponse` format
- `BaseEntity` — JPA mapped superclass with `id`, `createdAt`, `updatedAt`, `version` (optimistic locking)
- `SecurityConfig` — Spring Security + OAuth2 Client (configured in `backend/src/main/java/com/holodos/common/infrastructure/`)

### Key Domain Rules
- `StockEntry` = concrete batch; `Product` = template
- Movements cannot produce negative stock
- Shopping list auto-replenishment deduplicates active entries
- Google Keep sync is idempotent with circular-update guards
- `StubGoogleKeepClient` is the default; `HttpGoogleKeepClient` activates via `holodos.integrations.google-keep.enabled=true`

### Database
- PostgreSQL 16 via Docker; credentials `holodos/holodos`, db `holodos`
- Flyway manages schema: `backend/src/main/resources/db/migration/` (V1–V7)
- `spring.jpa.hibernate.ddl-auto=validate` — schema is never auto-created

### Testing Pattern
Unit tests use `@ExtendWith(MockitoExtension.class)` with constructor-injected services. Testcontainers (PostgreSQL) is available for integration tests. Tests live at `backend/src/test/java/com/holodos/<module>/application/`.

## Flutter Architecture

State management via Riverpod. Offline-first: SQLite (`sqflite`) is the source of truth, synced to backend via Dio. Code generation used for models (`freezed`, `json_serializable`).

```
mobile/lib/
├── core/        # Config, network & DB setup
├── features/    # dashboard, inventory, shopping (feature slices)
├── data/        # Dio client, repositories, Freezed models
└── shared/      # Reusable widgets
```
