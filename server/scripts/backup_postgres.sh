#!/usr/bin/env sh
set -eu

mkdir -p backups
STAMP="$(date +%Y%m%d_%H%M%S)"
FILE="backups/novelforge_${STAMP}.sql.gz"

docker compose exec -T db sh -c \
  'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges' \
  | gzip > "$FILE"

echo "Backup created: $FILE"
