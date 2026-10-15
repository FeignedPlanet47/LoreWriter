# Развертывание NovelForge backend в облаке

Backend уже использует настоящую PostgreSQL, FastAPI, Alembic-миграции, JWT, Argon2id, подтверждение email по SMTP и общий доступ к историям.

## Что именно нужно размещать

Обычное файловое облако (Google Drive, Dropbox, S3-бакет как единственный сервис) не может исполнять Python API. Нужна вычислительная среда:

1. VPS/виртуальная машина с Docker; или
2. облачная платформа, которая запускает Docker-контейнер; или
3. отдельно контейнер API + managed PostgreSQL.

## Вариант A: один VPS, PostgreSQL рядом с API

На VPS установите Docker и Docker Compose, скопируйте папку `server`, создайте `.env` и запустите:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

В этом режиме:

- Caddy слушает 80/443 и автоматически получает TLS-сертификат;
- FastAPI доступен только через Caddy;
- PostgreSQL не публикуется наружу;
- данные PostgreSQL хранятся в Docker volume `novelforge_pgdata`.

В `.env` обязательно задайте:

```env
ENVIRONMENT=production
API_DOMAIN=api.your-domain.com
POSTGRES_PASSWORD=...
JWT_SECRET=...
EMAIL_CODE_SECRET=...
SMTP_HOST=...
SMTP_USERNAME=...
SMTP_PASSWORD=...
SMTP_FROM_EMAIL=...
```

## Вариант B: API в облаке + managed PostgreSQL

Создайте PostgreSQL у выбранного облачного провайдера и получите строку подключения. В `.env` задайте:

```env
DATABASE_URL=postgresql://USER:PASSWORD@HOST:5432/DATABASE?sslmode=require
ENVIRONMENT=production
JWT_SECRET=...
EMAIL_CODE_SECRET=...
SMTP_HOST=...
SMTP_USERNAME=...
SMTP_PASSWORD=...
SMTP_FROM_EMAIL=...
```

При наличии `DATABASE_URL` значения `POSTGRES_HOST`, `POSTGRES_USER` и другие параметры соединения не используются API.

Для контейнерного запуска без локальной БД:

```bash
docker compose -f docker-compose.cloud.yml up -d --build
```

Многие облачные платформы сами задают переменную `PORT`. Dockerfile это поддерживает.

## Миграции базы

При каждом запуске контейнера автоматически выполняется:

```bash
alembic upgrade head
```

Поэтому таблицы пользователей, токенов, email-кодов, историй, редакторов/читателей и ссылок общего доступа создаются автоматически.

## Проверка

Жив ли API:

```text
GET /health
```

Готова ли база данных:

```text
GET /ready
```

`/ready` выполняет реальный `SELECT 1` в PostgreSQL и возвращает 503, если соединения с БД нет.

## Домен Android-приложения

После размещения сервера замените release URL Android на адрес API, например:

```text
https://api.your-domain.com/api/v1/
```

Локальный `127.0.0.1`/`adb reverse` после этого для обычных пользователей не нужен.

## Резервные копии при self-hosted PostgreSQL

Создать backup:

```bash
cd server
./scripts/backup_postgres.sh
```

Восстановить:

```bash
cd server
./scripts/restore_postgres.sh backups/novelforge_YYYYMMDD_HHMMSS.sql.gz
```

Если используется managed PostgreSQL, лучше включить автоматические backup/PITR средствами облачного провайдера.

## Что нельзя публиковать в GitHub

Никогда не коммитьте:

- `.env`;
- SMTP password/app password;
- JWT_SECRET;
- EMAIL_CODE_SECRET;
- настоящий DATABASE_URL с паролем.

Коммитить можно `.env.example`.
