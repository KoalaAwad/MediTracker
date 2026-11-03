#!/bin/bash
# Replaces the live database with a backup made by backup.sh.
#   ./restore.sh db-backup/meditracker-20261102-030000.dump
# To restore from R2, download the .dump from the R2 dashboard into db-backup/ first.
set -euo pipefail

cd "$(dirname "$0")"
. ./env-get.sh

DUMP="${1:-}"
if [ -z "$DUMP" ] || [ ! -f "$DUMP" ]; then
  echo "Usage: ./restore.sh <file.dump>"
  exit 1
fi
DB_USER="$(env_get DB_USER)"
DB_USER="${DB_USER:-myuser}"

# check the file before touching anything
docker exec -i meditracker-db pg_restore --list < "$DUMP" > /dev/null

echo "This DELETES everything in the live database and replaces it with $DUMP."
read -r -p "Type 'restore' to continue: " answer
[ "$answer" = "restore" ] || { echo "Cancelled."; exit 1; }

docker compose stop backend
docker exec meditracker-db dropdb -U "$DB_USER" --if-exists meditracker
docker exec meditracker-db createdb -U "$DB_USER" meditracker
docker exec -i meditracker-db pg_restore -U "$DB_USER" -d meditracker --no-owner --exit-on-error < "$DUMP"
docker compose start backend

echo "Restored $DUMP. Backend restarted."
