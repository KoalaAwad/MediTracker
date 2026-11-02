#!/bin/bash
set -e

# Change into this script's directory
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "deploy/.env is missing. Copy .env.example to .env and fill it in."
  exit 1
fi

echo "=========================================="
echo "MediTracker Deployment"
echo "=========================================="

# Pass extra args through, e.g. ./deploy.sh --profile tunnel
docker compose "$@" up --build -d

# Show status
docker compose "$@" ps

echo "=========================================="
echo "Site: http://127.0.0.1:${FRONTEND_PORT:-8081} (on this machine)"
echo "=========================================="
