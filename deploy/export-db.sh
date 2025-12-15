#!/bin/bash
set -e

cd "$(dirname "$0")"

mkdir -p db-backup

echo "Exporting PostgreSQL database from running container..."
# Try common container names; adjust if needed
if docker ps --format '{{.Names}}' | grep -q '^meditracker-db$'; then
  docker exec -e PGPASSWORD=meditracker_pass meditracker-db pg_dump -U myuser -d meditracker > db-backup/meditracker-backup.sql
else
  # Fallback: try default compose name
  docker exec -e PGPASSWORD=meditracker_pass meditracker-postgres-1 pg_dump -U myuser -d meditracker > db-backup/meditracker-backup.sql
fi

echo "Backup written to deploy/db-backup/meditracker-backup.sql"

