#!/bin/bash
# Dumps the database, checks the dump is readable, keeps BACKUP_KEEP_DAYS of local copies
# and, if R2_BUCKET is set, uploads it to Cloudflare R2. Meant to run nightly from cron:
#   0 3 * * * /path/to/MediTracker/deploy/backup.sh >> /path/to/MediTracker/deploy/db-backup/backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")"
. ./env-get.sh

PING_URL="$(env_get BACKUP_PING_URL)"
ping() {
  # optional dead man's switch (e.g. healthchecks.io): alerts you if backups stop arriving
  if [ -n "$PING_URL" ]; then curl -fsS -m 10 --retry 3 "$PING_URL$1" > /dev/null || true; fi
}
trap 'echo "$(date -Is) backup FAILED"; ping /fail' ERR

DB_USER="$(env_get DB_USER)"
mkdir -p db-backup
FILE="meditracker-$(date +%Y%m%d-%H%M%S).dump"
OUT="db-backup/$FILE"

# custom format: compressed, and restorable with pg_restore (see restore.sh)
docker exec meditracker-db pg_dump -U "${DB_USER:-myuser}" -d meditracker -Fc > "$OUT.partial"
docker exec -i meditracker-db pg_restore --list < "$OUT.partial" > /dev/null
mv "$OUT.partial" "$OUT"
echo "$(date -Is) wrote $OUT ($(du -h "$OUT" | cut -f1))"

KEEP_DAYS="$(env_get BACKUP_KEEP_DAYS)"
find db-backup -name 'meditracker-*.dump' -mtime +"${KEEP_DAYS:-14}" -delete

R2_BUCKET="$(env_get R2_BUCKET)"
if [ -n "$R2_BUCKET" ]; then
  # rclone runs in a throwaway container, configured entirely from env, so nothing to install
  docker run --rm \
    -v "$PWD/db-backup:/data:ro" \
    -e RCLONE_CONFIG_R2_TYPE=s3 \
    -e RCLONE_CONFIG_R2_PROVIDER=Cloudflare \
    -e RCLONE_CONFIG_R2_ACCESS_KEY_ID="$(env_get R2_ACCESS_KEY_ID)" \
    -e RCLONE_CONFIG_R2_SECRET_ACCESS_KEY="$(env_get R2_SECRET_ACCESS_KEY)" \
    -e RCLONE_CONFIG_R2_ENDPOINT="https://$(env_get R2_ACCOUNT_ID).r2.cloudflarestorage.com" \
    -e RCLONE_CONFIG_R2_NO_CHECK_BUCKET=true \
    rclone/rclone:1.68.2 copyto "/data/$FILE" "r2:$R2_BUCKET/meditracker/$FILE"
  echo "$(date -Is) uploaded to r2:$R2_BUCKET/meditracker/$FILE"
fi

ping ""
