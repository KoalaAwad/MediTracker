#!/bin/bash
set -e

# Change into this script's directory
cd "$(dirname "$0")"

echo "=========================================="
echo "MediTracker Deployment"
echo "=========================================="

# Build and start services
docker-compose up --build -d

# Show status
docker-compose ps

echo "=========================================="
echo "Frontend: http://localhost"
echo "Backend:  http://localhost:8080"
echo "Database: localhost:5432"
echo "=========================================="

