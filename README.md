# Chess Backend Deployment

This project is a Spring Boot backend for multiplayer chess.

## Production environment variables

Set these variables in your hosting platform (Render/Heroku/Fly/K8s/etc):

- `SPRING_PROFILES_ACTIVE=prod`
- `PORT` (if your platform requires it)
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `REDIS_URL`
- `JWT_SECRET` (minimum 32 bytes)
- `GOOGLE_OAUTH_CLIENT_ID`
- `APP_CORS_ALLOWED_ORIGINS` (comma-separated origins)
- `APP_UPLOAD_DIR` (optional, defaults to `/tmp/uploads`)

Use `.env.example` as a template and never commit real values.

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

