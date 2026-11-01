#!/usr/bin/env sh
set -eu
mkdir -p backups
docker compose exec -T db pg_dump -U cage -d cage --format=custom > "backups/cage-$(date +%Y%m%d-%H%M%S).dump"
printf 'Database backup saved. Preserve the DATA_ENCRYPTION_KEY separately for restore.\n'
