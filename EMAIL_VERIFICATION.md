# NovelForge: подтверждение email

## Что добавлено

- регистрация без немедленной выдачи JWT;
- 6-значный одноразовый код;
- срок действия кода 10 минут;
- отдельный Android-экран подтверждения;
- повторная отправка кода;
- ограничение частоты отправки;
- ограничение числа неверных попыток;
- HMAC-SHA256 хранение кода вместо открытого значения;
- отдельный `EMAIL_CODE_SECRET`;
- запрет login/refresh/API-доступа неподтвержденным аккаунтам;
- Mailpit для локальной проверки писем;
- SMTP/STARTTLS/SMTPS для production.

## API

```text
POST /api/v1/auth/register
POST /api/v1/auth/verify-email
POST /api/v1/auth/resend-verification
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

## Проверка локально

```bash
cd server
cp .env.example .env
# замените POSTGRES_PASSWORD, JWT_SECRET и EMAIL_CODE_SECRET

docker compose up --build
```

После регистрации откройте:

```text
http://localhost:8025
```

Там будет письмо NovelForge с кодом.

## Production

1. Укажите реальный SMTP в `.env`.
2. Используйте только HTTPS для Android release API.
3. Не храните SMTP_PASSWORD, JWT_SECRET, EMAIL_CODE_SECRET и POSTGRES_PASSWORD в репозитории.
4. Выполните `alembic upgrade head`.
