# Holodos

Mobile-first home inventory and shopping management platform.

- `backend/` — Java 25 + Spring Boot 4, modular monolith REST API
- `mobile/` — Flutter (Dart) offline-first client with SQLite + sync
- `docs/` — Architecture and design documentation

## Quick Start

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Start backend (port 8080)
cd backend && mvn spring-boot:run

# 3. Run mobile app
cd mobile && flutter run
```

Swagger UI: http://localhost:8080/swagger-ui.html

---

## Google Tasks Integration

Sync the shopping list to Google Tasks so it's visible on any device via the Google Tasks app.

### 1. Create OAuth2 credentials in Google Console

1. Open [console.cloud.google.com](https://console.cloud.google.com) and create or select a project
2. **APIs & Services → Enable APIs** → enable **Google Tasks API**
3. **APIs & Services → Credentials → Create Credentials → OAuth 2.0 Client ID**
   - Application type: **Web application**
   - Authorized redirect URIs: `http://localhost:8080/api/integrations/google-tasks/oauth2/callback`
4. Copy the **Client ID** and **Client Secret**

### 2. Add yourself as a test user

Until the app passes Google verification (not needed for personal use), only explicitly added accounts can authorize:

1. **APIs & Services → OAuth consent screen → Test users → Add users**
2. Add the Gmail address(es) you want to use

### 3. Configure the backend

Set environment variables before starting:

```bash
export GOOGLE_TASKS_CLIENT_ID=your-client-id.apps.googleusercontent.com
export GOOGLE_TASKS_CLIENT_SECRET=your-client-secret
```

Or add directly to `backend/src/main/resources/application.yml`:

```yaml
holodos:
  integrations:
    google-tasks:
      client-id: "your-client-id"
      client-secret: "your-client-secret"
```

### 4. Authorize

With the backend running, get the authorization URL:

```bash
curl "http://localhost:8080/api/integrations/google-tasks/auth-url?userKey=slava"
# → {"authUrl": "https://accounts.google.com/o/oauth2/v2/auth?..."}
```

Open the returned `authUrl` in a browser, sign in with your Google account, and allow access.
The browser redirects back automatically — authorization is complete.

### 5. Multiple devices / accounts

Each person authorizes separately with their own `userKey` and Google account:

```bash
curl "http://localhost:8080/api/integrations/google-tasks/auth-url?userKey=slava"
curl "http://localhost:8080/api/integrations/google-tasks/auth-url?userKey=anna"
```

Everyone sees the same shopping list ("Holodos Shopping") in their own Google Tasks app.

### 6. Sync

Sync runs automatically every 5 minutes for all authorized users.

To trigger manually:

```bash
curl -X POST http://localhost:8080/api/integrations/google-tasks/sync \
  -H "Content-Type: application/json" \
  -d '{"userKey": "slava"}'
```

To check binding status:

```bash
curl http://localhost:8080/api/integrations/google-tasks/bindings
```

---

## Backend Commands

```bash
cd backend

mvn clean install                             # Full build
mvn test                                      # All tests
mvn test -Dtest=ProductServiceTest            # Single test class
mvn spring-boot:run                           # Start server
```

## Mobile Commands

```bash
cd mobile

flutter pub get    # Install dependencies
flutter run        # Run app
flutter test       # Run tests
flutter analyze    # Lint
```
