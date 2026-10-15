#!/usr/bin/env sh
set -eu

docker compose -f docker-compose.cloud.yml run --rm migrate
