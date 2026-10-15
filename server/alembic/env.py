import os
from logging.config import fileConfig
from urllib.parse import quote_plus

from alembic import context
from sqlalchemy import engine_from_config, pool
from sqlalchemy.engine import make_url

from app.models import Base

config = context.config


def _to_sync_url(raw_url: str) -> str:
    """Convert a provider PostgreSQL URL to psycopg format for Alembic."""
    url = make_url(raw_url.strip())
    query = dict(url.query)
    ssl_value = query.pop("ssl", None)
    if ssl_value and "sslmode" not in query:
        query["sslmode"] = ssl_value
    url = url.set(drivername="postgresql+psycopg", query=query)
    return url.render_as_string(hide_password=False)


def _migration_database_url() -> str:
    # Cloud/production: owner credential is supplied ONLY to the migration job.
    migration_url = os.getenv("DATABASE_MIGRATION_URL", "").strip()
    if migration_url:
        return _to_sync_url(migration_url)

    # Convenient fallback for local development or manual migrations.
    runtime_url = os.getenv("DATABASE_URL", "").strip()
    if runtime_url:
        return _to_sync_url(runtime_url)

    # Local Docker PostgreSQL fallback.
    db = os.getenv("POSTGRES_DB", "novelforge")
    user = quote_plus(os.getenv("POSTGRES_USER", "novelforge"))
    password = quote_plus(os.getenv("POSTGRES_PASSWORD", ""))
    host = os.getenv("POSTGRES_HOST", "db")
    port = int(os.getenv("POSTGRES_PORT", "5432"))
    return f"postgresql+psycopg://{user}:{password}@{host}:{port}/{db}"


config.set_main_option("sqlalchemy.url", _migration_database_url())
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

target_metadata = Base.metadata


def run_migrations_offline():
    context.configure(
        url=config.get_main_option("sqlalchemy.url"),
        target_metadata=target_metadata,
        literal_binds=True,
        compare_type=True,
    )
    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online():
    connectable = engine_from_config(
        config.get_section(config.config_ini_section),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )
    with connectable.connect() as connection:
        context.configure(
            connection=connection,
            target_metadata=target_metadata,
            compare_type=True,
        )
        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
