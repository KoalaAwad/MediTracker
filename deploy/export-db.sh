#!/bin/bash
set -e

cd "$(dirname "$0")"

# DB_USER comes from deploy/.env
set -a
[ -f .env ] && . ./.env
set +a

mkdir -p db-backup

OUT="db-backup/meditracker-$(date +%Y%m%d-%H%M%S).sql"

echo "Exporting PostgreSQL database from running container..."
# Socket connections inside the postgres container need no password
docker exec meditracker-db pg_dump -U "${DB_USER:-myuser}" -d meditracker > "$OUT"

echo "Backup written to deploy/$OUT"
