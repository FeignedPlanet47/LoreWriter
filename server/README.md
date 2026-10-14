# NovelForge Server

FastAPI + PostgreSQL backend для аккаунтов и синхронизации NovelForge.

## 1. Настройка

```bash
cp .env.example .env
```

В `.env` обязательно замените:

- `POSTGRES_PASSWORD` на длинный случайный пароль;
- `JWT_SECRET` на случайный секрет минимум 64 символа.

Например:

```bash
openssl rand -hex 64
```

Для production установите:

```text
ENVIRONMENT=production
```

В production Swagger/OpenAPI отключаются.

## 2. Запуск

```bash
docker compose up --build
```

Контейнер API сам выполняет:

```bash
alembic upgrade head
```

API: `http://localhost:8000`  
Health: `http://localhost:8000/health`  
Swagger в development: `http://localhost:8000/docs`

PostgreSQL наружу не публикуется.

## 3. API

### Авторизация

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

### Истории

```text
GET    /api/v1/stories
POST   /api/v1/stories
PUT    /api/v1/stories/{story_id}
DELETE /api/v1/stories/{story_id}
```

История хранится как JSONB snapshot: схема, узлы, форматирование, переходы, персонажи, рукопись и Word-текст.

`PUT` принимает `baseRevision`. Если клиент пытается отправить устаревшую версию, сервер отвечает `409 story_revision_conflict`, а не перезаписывает более новую копию.

## 4. Безопасность

- пароли: Argon2id hash через `pwdlib`, plaintext не сохраняется;
- отсутствующий email при login также проходит dummy hash verify, чтобы уменьшить различие времени ответа;
- access token: короткоживущий HS256 JWT с `iss`, `aud`, `iat`, `exp`;
- refresh token: криптографически случайный, ротируется при каждом refresh;
- в БД хранится только SHA-256 hash refresh token;
- refresh-строка блокируется `SELECT ... FOR UPDATE` во время ротации;
- каждая операция с историей проверяет `owner_id`;
- секреты только в `.env`;
- production работает только за HTTPS reverse proxy;
- рекомендуется rate limiting для `/auth/login`, `/auth/register`, `/auth/refresh` на Caddy/Nginx/Traefik/WAF.

## 5. Android

Эмулятор в debug использует:

```text
http://10.0.2.2:8000/api/v1/
```

Для реального телефона в локальной сети укажите IP компьютера в `data/build.gradle.kts`.

**HTTP debug предназначен только для локальной разработки.** Для staging/production используйте HTTPS.

Release URL в проекте является заглушкой:

```text
https://api.example.com/api/v1/
```

Замените ее на свой настоящий домен до сборки release.

## 6. Production

Минимум:

1. HTTPS/TLS через Caddy, Nginx или Traefik.
2. Не публиковать порт PostgreSQL в интернет.
3. `ENVIRONMENT=production`.
4. Отдельные длинные `POSTGRES_PASSWORD` и `JWT_SECRET`.
5. Rate limiting на auth endpoints.
6. Регулярные резервные копии PostgreSQL.
7. Логи не должны содержать пароли, Authorization headers или raw refresh tokens.

## 7. Готовый production HTTPS через Caddy

1. Укажите DNS `A/AAAA` для домена API на сервер.
2. В `.env` задайте:

```text
API_DOMAIN=api.example.com
ENVIRONMENT=production
```

3. Запустите:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

В production compose наружу публикуются только 80/443 Caddy. API и PostgreSQL остаются во внутренней Docker-сети. Caddy автоматически получает и обновляет TLS-сертификат для корректно настроенного домена.

## Подтверждение email

Регистрация теперь двухэтапная:

1. `POST /api/v1/auth/register` создает неподтвержденный аккаунт и отправляет 6-значный код.
2. `POST /api/v1/auth/verify-email` подтверждает код и только после этого выдает access/refresh tokens.

Дополнительный endpoint:

- `POST /api/v1/auth/resend-verification` — повторная отправка кода.

Защита:

- код действует 10 минут;
- предыдущий код аннулируется при выдаче нового;
- максимум 5 неверных попыток;
- повторная отправка не чаще раза в 60 секунд;
- максимум 5 писем в час на пользователя;
- сам код не хранится в БД, хранится HMAC-SHA256 с отдельным `EMAIL_CODE_SECRET`;
- пароль по-прежнему хранится только как Argon2id-хеш;
- вход и refresh запрещены до подтверждения email.

### Локальная разработка

`docker compose up --build` поднимает Mailpit. Письма не уходят в интернет, а доступны в браузере:

```text
http://localhost:8025
```

Это удобно для проверки регистрации без реального SMTP.

### Production SMTP

В production `.env` замените тестовые SMTP-параметры на данные вашего почтового провайдера. Например для SMTP с STARTTLS:

```env
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=no-reply@example.com
SMTP_PASSWORD=your_generated_smtp_password
SMTP_FROM_EMAIL=no-reply@example.com
SMTP_FROM_NAME=NovelForge
SMTP_USE_TLS=true
SMTP_USE_SSL=false
```

Для SMTPS/SSL обычно используется порт 465:

```env
SMTP_USE_TLS=false
SMTP_USE_SSL=true
SMTP_PORT=465
```

Не коммитьте `.env` и SMTP-пароль в Git.

После обновления уже существующего сервера примените миграцию:

```bash
alembic upgrade head
```

Миграция `0002_email_verification.py` помечает старые аккаунты как уже подтвержденные, чтобы существующие пользователи не потеряли доступ.
