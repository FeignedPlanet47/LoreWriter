from urllib.parse import quote_plus

from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy.engine import make_url


class Settings(BaseSettings):
    # Local/self-hosted PostgreSQL fallback.
    postgres_db: str = "novelforge"
    postgres_user: str = "novelforge"
    postgres_password: str = Field(default="", min_length=0)
    postgres_host: str = "db"
    postgres_port: int = 5432

    # Runtime database URL. In production this MUST use the restricted
    # novelforge_app role, not the database owner.
    external_database_url: str = Field(default="", validation_alias="DATABASE_URL")

    jwt_secret: str = Field(min_length=64)
    jwt_issuer: str = "novelforge-api"
    jwt_audience: str = "novelforge-android"
    environment: str = "development"
    access_token_minutes: int = Field(default=15, ge=5, le=60)
    refresh_token_days: int = Field(default=30, ge=1, le=90)

    email_code_secret: str = Field(min_length=32)
    email_verification_code_minutes: int = Field(default=10, ge=3, le=30)
    email_verification_resend_seconds: int = Field(default=60, ge=30, le=600)
    email_verification_max_attempts: int = Field(default=5, ge=3, le=20)
    email_verification_max_sends_per_hour: int = Field(default=5, ge=1, le=20)

    smtp_host: str = Field(min_length=1)
    smtp_port: int = Field(default=587, ge=1, le=65535)
    smtp_username: str = ""
    smtp_password: str = ""
    smtp_from_email: str = Field(min_length=3)
    smtp_from_name: str = "NovelForge"
    smtp_use_tls: bool = True
    smtp_use_ssl: bool = False
    smtp_timeout_seconds: int = Field(default=15, ge=5, le=60)

    db_pool_size: int = Field(default=5, ge=1, le=30)
    db_max_overflow: int = Field(default=10, ge=0, le=50)
    db_pool_recycle_seconds: int = Field(default=1800, ge=60, le=7200)

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    @model_validator(mode="after")
    def validate_settings(self):
        if self.smtp_use_tls and self.smtp_use_ssl:
            raise ValueError("SMTP_USE_TLS and SMTP_USE_SSL cannot both be true")
        if self.smtp_username and not self.smtp_password:
            raise ValueError("SMTP_PASSWORD is required when SMTP_USERNAME is set")
        if not self.external_database_url and len(self.postgres_password) < 16:
            raise ValueError(
                "POSTGRES_PASSWORD must contain at least 16 characters when DATABASE_URL is not set"
            )
        return self

    def _external_url(self, async_driver: bool) -> str:
        url = make_url(self.external_database_url.strip())
        query = dict(url.query)

        if async_driver:
            # asyncpg uses `ssl`, while Neon and many providers publish `sslmode`.
            sslmode = query.pop("sslmode", None)
            if sslmode and "ssl" not in query:
                query["ssl"] = sslmode
            # channel_binding is a libpq parameter and is not accepted by asyncpg.
            query.pop("channel_binding", None)
            url = url.set(drivername="postgresql+asyncpg", query=query)
        else:
            ssl_value = query.pop("ssl", None)
            if ssl_value and "sslmode" not in query:
                query["sslmode"] = ssl_value
            url = url.set(drivername="postgresql+psycopg", query=query)

        return url.render_as_string(hide_password=False)

    @property
    def database_url(self) -> str:
        """Runtime URL used by FastAPI only."""
        if self.external_database_url:
            return self._external_url(async_driver=True)

        user = quote_plus(self.postgres_user)
        password = quote_plus(self.postgres_password)
        return (
            f"postgresql+asyncpg://{user}:{password}@"
            f"{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )


settings = Settings()
