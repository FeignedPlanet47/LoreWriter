from urllib.parse import quote_plus

from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    postgres_db: str = "novelforge"
    postgres_user: str = "novelforge"
    postgres_password: str = Field(min_length=16)
    postgres_host: str = "db"
    postgres_port: int = 5432

    jwt_secret: str = Field(min_length=64)
    jwt_issuer: str = "novelforge-api"
    jwt_audience: str = "novelforge-android"
    environment: str = "development"
    access_token_minutes: int = Field(default=15, ge=5, le=60)
    refresh_token_days: int = Field(default=30, ge=1, le=90)

    # Отдельный pepper для HMAC-хеша коротких email-кодов.
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

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    @model_validator(mode="after")
    def validate_smtp(self):
        if self.smtp_use_tls and self.smtp_use_ssl:
            raise ValueError("SMTP_USE_TLS and SMTP_USE_SSL cannot both be true")
        if self.smtp_username and not self.smtp_password:
            raise ValueError("SMTP_PASSWORD is required when SMTP_USERNAME is set")
        return self

    @property
    def database_url(self) -> str:
        user = quote_plus(self.postgres_user)
        password = quote_plus(self.postgres_password)
        return (
            f"postgresql+asyncpg://{user}:{password}@"
            f"{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )


settings = Settings()
