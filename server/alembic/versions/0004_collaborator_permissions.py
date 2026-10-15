"""collaborator permissions

Revision ID: 0004
Revises: 0003
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import JSONB
revision = "0004"
down_revision = "0003"
branch_labels = None
depends_on = None

def upgrade():
    op.add_column("story_collaborators", sa.Column("permissions", JSONB(), nullable=True))

def downgrade():
    op.drop_column("story_collaborators", "permissions")
