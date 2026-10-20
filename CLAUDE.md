# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Holodos** — home inventory and shopping management app.

- `backend/` — Java 21 + Spring Boot 4 (4.0.4) modular monolith REST API; also serves the web UIs
- `docs/` — architecture notes (`architecture.md` is the original design proposal and partly aspirational — trust the code over it)
- `scripts/` — `export-db.sh` / `import-db.sh` for dumping/restoring the Docker Compose Postgres

The Flutter `mobile/` client was **removed** (commit 095d1b7) because the target device is an iPad 3 (iOS 9.3.5). README and `docs/` still mention Flutter; ignore those parts.

## Commands

```bash
docker compose up -d postgres          # PostgreSQL only (local dev)
docker compose up -d                   # Postgres + backend container

cd backend
mvn spring-boot:run                    # Start backend on :8080
mvn clean install                      # Full build
mvn test                               # All tests
mvn test -Dtest=ProductServiceTest                # Single class
mvn test -Dtest=ProductServiceTest#methodName     # Single method
```

Maven (`pom.xml`) is the real build — the Dockerfile uses it. `build.gradle.kts` is stale (Boot milestone, missing deps) and unused.

Swagger UI: `http://localhost:8080/swagger-ui.html`, OpenAPI JSON at `/api/docs`.

## Web Clients (no build step)

Plain static HTML in `backend/src/main/resources/static/`, served by Spring Boot:
- `app/index.html` — main touch UI for iPad 3. Single file, no external dependencies (hand-written CSS, inline SVG icon sprite), **ES5 only**: no `let`/`const`, arrow functions, template literals, `fetch` or other features Safari 9 can't run. HTTP goes through `XMLHttpRequest`. CSS likewise: no grid, flex `gap`, CSS variables or `vh` layout; keep `-webkit-` flex prefixes. Icons are `<svg><use xlink:href="#i-…">` (bare `href` doesn't work in Safari 9); avoid emoji newer than iOS 9 (e.g. 🧊, 🥤 render as boxes).
- `admin/index.html` — catalog CRUD admin panel. Same constraints: single file, ES5 + XHR, no CDN.

Both call the REST API at relative `/api/...` using the session cookie.

## Security

Everything except Swagger and `/actuator/health` requires Google OAuth2 login (`SecurityConfig`). Unauthenticated `/api/**` requests get 401 instead of a redirect. Login is restricted to the whitelist `holodos.security.allowed-emails` in `application.yml` (enforced in `HolodosOAuth2UserService`, which also upserts the `users` table). Requires `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` env vars. CSRF is disabled; CORS origins come from `HOLODOS_CORS_ALLOWED_ORIGINS`.

## Backend Architecture

Each module under `com.holodos.<module>` uses `api` / `application` / `domain` / `infrastructure` packages.

**Modules:** `common`, `catalog`, `inventory`, `shopping`, `purchases`, `notifications`, `reports`, `export`, `media`, `settings`, `integrations.googlekeep`, `integrations.googletasks`

Actual conventions (differ from the textbook layering):
- Repositories are Spring Data `JpaRepository` interfaces living in `infrastructure/`, injected directly into services.
- Modules call each other directly — services import other modules' entities and repositories (e.g. `PurchaseService` uses `StockEntryRepository` and `ShoppingListItemRepository`; `InventoryService` calls `ShoppingListService` for auto-replenishment).
- DTOs are records grouped in one `*Dtos.java` file per module's `api/` package; services often accept/return these DTOs.

### Cross-Cutting (`common`)
- Mutations publish `OperationLogEvent` via `DomainEventPublisher`; `OperationLogEventListener` writes to `operation_log` **after commit** (`@TransactionalEventListener`).
- `CorrelationIdFilter` (`X-Correlation-ID`), `GlobalExceptionHandler` → `ErrorResponse`, `NotFoundException` → 404.
- `BaseEntity` — `id`, `createdAt`, `updatedAt`, `version` (optimistic locking).
- Caffeine cache for catalog dictionaries (cache names in `application.yml`).

### Key Domain Rules
- `Product` = template; `StockEntry` = concrete batch. Storage places are flat (no hierarchy).
- Movements cannot produce negative stock.
- Shopping-list auto-replenishment deduplicates active entries.
- Buying a shopping item (`purchases`) creates stock entries.

### Pluggable Adapters
- Google Keep: `StubGoogleKeepClient` by default; `HttpGoogleKeepClient` when `holodos.integrations.google-keep.enabled=true`. Sync is idempotent with circular-update guards.
- Google Tasks: real HTTP client, per-user OAuth keyed by `userKey`; setup steps in README.
- Media: filesystem storage by default (`HOLODOS_MEDIA_PATH`), S3/MinIO when `holodos.media.provider=S3` (the commented S3 example in `application.yml` sits under `integrations` by mistake; the real prefix is `holodos.media`).

### Schedulers (`@EnableScheduling`)
- Google Tasks sync — every 5 min
- Google Keep retry — every 20 min
- Inventory expiry/low-stock notifications — every 30 min

### Database
- PostgreSQL 16; credentials `holodos/holodos`, db `holodos`.
- Flyway migrations in `backend/src/main/resources/db/migration/` (currently V1–V11). Add a new `V<n+1>__*.sql`; never edit applied ones.
- `ddl-auto=validate` — entity changes require a matching migration or startup fails.

### Testing
Mostly unit tests: `@ExtendWith(MockitoExtension.class)` with mocked repositories, under `backend/src/test/java/com/holodos/<module>/`. `ApplicationIntegrationTest` boots the whole app against Testcontainers PostgreSQL (skipped when Docker is unavailable).

## Deployment

No GitHub Actions. The home server pulls itself: cron runs `~/bin/auto-deploy.sh holodos main` every 5 minutes, which fetches `origin/main` and, if there are new commits, fast-forwards `~/holodos`, runs `docker compose build backend && docker compose up -d` and checks `/actuator/health`. Merging to `main` = deploying to production within ~5 minutes. The script skips the deploy if the server checkout has uncommitted changes to tracked files, so configure the server only through `.env` (e.g. `HOLODOS_BIND_ADDRESS`), never by editing `docker-compose.yml` there. Env vars for the compose stack are listed in `.env.example`.
