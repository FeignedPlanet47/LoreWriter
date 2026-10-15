# NovelForge: separate DB roles

## Runtime API

`server/.env` must contain the Neon connection string for the restricted role:

```env
DATABASE_URL=postgresql://novelforge_app:...@.../novelforge?sslmode=require
```

FastAPI uses only this URL.

## Alembic migrations

Create `server/.env.migrations` from `.env.migrations.example` and paste the Neon connection string for `novelforge_owner`:

```env
DATABASE_MIGRATION_URL=postgresql://novelforge_owner:...@.../novelforge?sslmode=require
```

The API container does not load `.env.migrations`.

## Cloud start

```bash
cp .env.example .env
cp .env.migrations.example .env.migrations
# fill both files with real credentials

docker compose -f docker-compose.cloud.yml up -d --build
```

The one-shot `migrate` service upgrades the schema first. Only after it exits successfully does `api` start.

## Run only a migration later

```bash
./scripts/migrate_cloud.sh
```

## Security

Never commit `.env` or `.env.migrations`. `novelforge_app` has DML privileges but no schema CREATE privilege. `novelforge_owner` is reserved for schema migrations.
