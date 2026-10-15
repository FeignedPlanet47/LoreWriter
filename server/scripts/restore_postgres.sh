#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 backups/file.sql.gz" >&2
  exit 1
fi

FILE="$1"
if [ ! -f "$FILE" ]; then
  echo "Backup not found: $FILE" >&2
  exit 1
fi

gzip -dc "$FILE" | docker compose exec -T db sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

echo "Restore completed from: $FILE"
