# Holodos — Implementation Plan

## Stack
- Backend: Java 25, Spring Boot 4, PostgreSQL, Flyway, Spring Data JPA, Spring Cache, Caffeine, Spring Security, OpenAPI
- Mobile: Flutter, SQLite, Riverpod
- Architecture: modular monolith backend + Flutter mobile app
- No Redis
- No hierarchical storage places

## Steps

- [x] 1. Propose the repository structure
- [x] 2. Create backend project bootstrap
- [x] 3. Create Flutter project bootstrap
- [x] 4. Add Docker Compose for PostgreSQL and S3-compatible local storage if useful
- [x] 5. Add Flyway initial migrations (V1–V8)
- [x] 6. Implement catalog module (StoragePlace, UnitOfMeasure, Category, Store, Product)
- [x] 7. Add REST CRUD endpoints
- [x] 8. Add DTOs, validation, mapping, service layer
- [x] 9. Add tests
- [x] 10. Add README with local run instructions
