"""story sharing and collaborator roles

Revision ID: 0003
Revises: 0002
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "0003"
down_revision = "0002"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "story_collaborators",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column(
            "story_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("stories.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("role", sa.String(16), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint(
            "story_id",
            "user_id",
            name="uq_story_collaborator_story_user",
        ),
    )
    op.create_index(
        "ix_story_collaborators_story_id",
        "story_collaborators",
        ["story_id"],
    )
    op.create_index(
        "ix_story_collaborators_user_id",
        "story_collaborators",
        ["user_id"],
    )

    op.create_table(
        "story_share_links",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column(
            "story_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("stories.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column(
            "created_by",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("token_hash", sa.String(64), nullable=False),
        sa.Column("role", sa.String(16), nullable=False),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index(
        "ix_story_share_links_story_id",
        "story_share_links",
        ["story_id"],
    )
    op.create_index(
        "ix_story_share_links_token_hash",
        "story_share_links",
        ["token_hash"],
        unique=True,
    )


def downgrade():
    op.drop_index("ix_story_share_links_token_hash", table_name="story_share_links")
    op.drop_index("ix_story_share_links_story_id", table_name="story_share_links")
    op.drop_table("story_share_links")

    op.drop_index("ix_story_collaborators_user_id", table_name="story_collaborators")
    op.drop_index("ix_story_collaborators_story_id", table_name="story_collaborators")
    op.drop_table("story_collaborators")
