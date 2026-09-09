# Chess Backend Deployment

This project is a Spring Boot backend for multiplayer chess.

## Local run

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
./mvnw.cmd spring-boot:run
```

The default `dev` profile starts with an embedded H2 database, so you do not
need a local PostgreSQL server just to launch the app. If you want to point the
dev profile at PostgreSQL instead, set `DB_URL`, `DB_USER`, and `DB_PASSWORD`.

## Production run

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
./mvnw.cmd clean package
java -jar target/chess-0.0.1-SNAPSHOT.jar
```

## Security notes

- API CORS and WebSocket origins are controlled by `APP_CORS_ALLOWED_ORIGINS`.
- OAuth client ID is externalized via `GOOGLE_OAUTH_CLIENT_ID`.
- Secrets are read from environment variables in production profile.
- Sign-out uses `POST /api/auth/logout` with a bearer token and revokes that JWT until it expires.

