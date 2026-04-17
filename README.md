# Chess Backend Deployment

This project is a Spring Boot backend for multiplayer chess.

## Local run

```powershell
$env:SPRING_PROFILES_ACTIVE='dev'
./mvnw.cmd spring-boot:run
```

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

