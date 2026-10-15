"""add unique login to users

Revision ID: 0005
Revises: 0004
"""

from __future__ import annotations

import re
import uuid

from alembic import op
import sqlalchemy as sa


revision = "0005"
down_revision = "0004"
branch_labels = None
depends_on = None


def _legacy_login(email: str, user_id: uuid.UUID, used: set[str]) -> str:
    local = (email or "").split("@", 1)[0].lower()
    base = re.sub(r"[^a-z0-9._-]+", "_", local).strip("._-")
    if len(base) < 3:
        base = f"user_{str(user_id).replace('-', '')[:8]}"
    base = base[:32]

    candidate = base
    if candidate in used:
        suffix = "_" + str(user_id).replace("-", "")[:6]
        candidate = (base[: 32 - len(suffix)] + suffix).lower()

    counter = 2
    while candidate in used:
        suffix = f"_{counter}"
        candidate = base[: 32 - len(suffix)] + suffix
        counter += 1

    used.add(candidate)
    return candidate


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column("login", sa.String(length=32), nullable=True),
    )

    connection = op.get_bind()
    rows = connection.execute(
        sa.text("SELECT id, email FROM users ORDER BY created_at, id")
    ).mappings().all()

    used: set[str] = set()
    for row in rows:
        login = _legacy_login(row["email"], row["id"], used)
        connection.execute(
            sa.text(
                "UPDATE users "
                "SET login = :login, display_name = :login "
                "WHERE id = :user_id"
            ),
            {"login": login, "user_id": row["id"]},
        )

    op.alter_column(
        "users",
        "login",
        existing_type=sa.String(length=32),
        nullable=False,
    )
    op.create_index("ix_users_login", "users", ["login"], unique=True)


def downgrade() -> None:
    op.drop_index("ix_users_login", table_name="users")
    op.drop_column("users", "login")
