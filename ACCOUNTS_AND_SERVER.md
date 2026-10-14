# Аккаунты и синхронизация: что изменено

## Android

- `AuthScreen` / `AuthViewModel`: регистрация, вход, восстановление сессии, выход.
- `SecureSessionStore`: AES/GCM + Android Keystore для access/refresh token и локального профиля.
- `AuthInterceptor` + `TokenAuthenticator`: Bearer token и автоматический refresh.
- `StoryRepositoryImpl`: offline-first синхронизация полного snapshot истории.
- Room v5: `ownerUserId`, `cloudId`, `cloudRevision`, `isDirty`, `pendingDelete`.
- `StoriesScreen`: аккаунт, статус облака, ручная синхронизация, выход.
- Локальный кэш разделен по `ownerUserId`.

## Server

- `server/app/auth.py`: register/login/refresh/logout/me.
- `server/app/stories.py`: CRUD историй с проверкой владельца.
- `server/app/security.py`: Argon2id, JWT, refresh token hashing.
- `server/app/models.py`: users, refresh_tokens, stories.
- `server/alembic`: миграция PostgreSQL.
- `server/docker-compose.yml`: API + PostgreSQL.

## Важно перед release

Заменить в `data/build.gradle.kts`:

```text
https://api.example.com/api/v1/
```

на настоящий HTTPS URL.
